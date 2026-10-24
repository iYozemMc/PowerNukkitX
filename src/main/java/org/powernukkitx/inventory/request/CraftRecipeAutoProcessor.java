package org.powernukkitx.inventory.request;

import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.RecipeIngredient;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.AutoCraftRecipeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ConsumeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType;
import org.jetbrains.annotations.Nullable;
import org.powernukkitx.Player;
import org.powernukkitx.event.inventory.CraftItemEvent;
import org.powernukkitx.inventory.CreativeOutputInventory;
import org.powernukkitx.inventory.Inventory;
import org.powernukkitx.item.Item;
import org.powernukkitx.recipe.Recipe;
import org.powernukkitx.recipe.UserDataShapelessRecipe;
import org.powernukkitx.recipe.descriptor.ItemDescriptor;
import org.powernukkitx.recipe.descriptor.ItemDescriptorType;
import org.powernukkitx.registry.Registries;

import java.util.ArrayList;
import java.util.List;

import static org.powernukkitx.inventory.request.CraftRecipeActionProcessor.RECIPE_DATA_KEY;

@Slf4j
public class CraftRecipeAutoProcessor implements ItemStackRequestActionProcessor<AutoCraftRecipeAction> {
    @Override
    public ItemStackRequestActionType getType() {
        return ItemStackRequestActionType.CRAFT_RECIPE_AUTO;
    }

    @Nullable
    @Override
    public ActionResponse handle(AutoCraftRecipeAction action, Player player, ItemStackRequestContext context) {
        Recipe recipe = Registries.RECIPE.getRecipeByNetworkId(action.getRecipeNetId().getRawId());
        if (recipe == null) {
            log.debug("Rejecting auto craft request for unknown recipe network id {} (recipe registry {})",
                    action.getRecipeNetId().getRawId(), Registries.RECIPE.isEnabled() ? "enabled" : "disabled");
            return context.error();
        }

        List<ItemDescriptor> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            log.debug("Rejecting auto craft request for recipe {} which carries no server side ingredients", recipe.getRecipeId());
            return context.error();
        }

        int timesCrafted = resolveTimesCrafted(action);
        if (timesCrafted < 1) {
            log.debug("Rejecting auto craft request for recipe {} with invalid craft count {}", recipe.getRecipeId(), timesCrafted);
            return context.error();
        }

        Item[] eventItems = action.getIngredients().stream().map(RecipeIngredient::toItem).map(Item::fromNetwork).toArray(Item[]::new);

        CraftItemEvent craftItemEvent = new CraftItemEvent(player, eventItems, recipe, timesCrafted);
        player.getServer().getPluginManager().callEvent(craftItemEvent);
        if (craftItemEvent.isCancelled()) {
            return context.error();
        }

        var consumeActions = findAllConsumeActions(context.getItemStackRequest().getActions(), context.getCurrentActionIndex() + 1);
        if (!consumeActionsCoverRecipe(player, ingredients, consumeActions, timesCrafted)) {
            log.warn("Mismatched recipe! Network id: {},Recipe name: {},Recipe type: {}", action.getRecipeNetId(), recipe.getRecipeId(), recipe.getType());
            return context.error();
        }

        context.put(RECIPE_DATA_KEY, recipe);
        player.getRecipeBook().unlock(recipe);

        if (recipe.getResults().size() == 1) {
            Item output = recipe.getResults().getFirst().clone();
            if (recipe instanceof UserDataShapelessRecipe) {
                for (Item inputItem : eventItems) {
                    if (!inputItem.isNull() && inputItem.hasNbt()) {
                        output.setNbtBytes(inputItem.getNbtBytes());
                        break;
                    }
                }
            }
            int outputCount = output.getCount() * timesCrafted;
            if (outputCount > output.getMaxStackSize()) {
                log.warn("Rejecting auto craft request for recipe {}: output count {} exceeds the maximum stack size {}",
                        recipe.getRecipeId(), outputCount, output.getMaxStackSize());
                return context.error();
            }
            output.setCount(outputCount);
            CreativeOutputInventory createdOutput = player.getCreativeOutputInventory();
            createdOutput.setItem(0, output.clone().autoAssignStackNetworkId(), false);
        }
        return null;
    }

    private static int resolveTimesCrafted(AutoCraftRecipeAction action) {
        int timesCrafted = action.getTimesCrafted();
        if (timesCrafted > 0) {
            return timesCrafted;
        }
        int numberOfRequestedCrafts = action.getNumberOfRequestedCrafts();
        if (numberOfRequestedCrafts > 0) {
            return numberOfRequestedCrafts;
        }
        log.debug("Auto craft request carries no usable craft count (timesCrafted {}, numberOfRequestedCrafts {}), falling back to 1",
                timesCrafted, numberOfRequestedCrafts);
        return 1;
    }

    private static boolean consumeActionsCoverRecipe(Player player, List<ItemDescriptor> ingredients, List<ConsumeAction> consumeActions, int timesCrafted) {
        ConsumedSource[] sources = new ConsumedSource[consumeActions.size()];
        int[] amounts = new int[consumeActions.size()];
        for (int i = 0; i < consumeActions.size(); i++) {
            sources[i] = resolveConsumedSource(player, consumeActions.get(i));
            amounts[i] = consumeActions.get(i).getAmount();
        }
        return consumeActionsCoverRecipe(ingredients, sources, amounts, timesCrafted);
    }

    /**
    * Whether the consume actions of a request pay for the recipe in full.
    * <p>
    * Nothing stops a request from naming the same slot in several consume actions, and between them
    * they can only take what that slot holds, so each slot is credited once however many actions
    * name it. Crediting it in full per action would let a request claim an ingredient it never pays
    * for.
    *
    * @param ingredients  what one craft of the recipe needs
    * @param sources      the slot each consume action names, {@code null} where it did not resolve
    * @param amounts      how much each consume action claims to take, in the same order
    * @param timesCrafted how many times the recipe is being crafted
    * @return whether every ingredient is covered
    */
    static boolean consumeActionsCoverRecipe(List<ItemDescriptor> ingredients, ConsumedSource[] sources,
                                             int [] amounts, int timesCrafted) {
        int[] available = new int[sources.length];
        for (int i = 0; i < sources.length; i++) {
            if (sources[i] == null) {
                continue;
            }
            int remaining = sources[i].item().getCount();
            for (int earlier = 0; earlier < i; earlier++) {
                if (sources[earlier] != null && sources[earlier].isSameSlotAs(sources[i])){
                    remaining -= available[earlier];
                }
            }
            available[i] = Math.min(amounts[i], Math.max(remaining, 0));
        }

        for (ItemDescriptor ingredient : ingredients) {
            if (ingredient.getType() != ItemDescriptorType.DEFAULT && ingredient.getType() != ItemDescriptorType.ITEM_TAG) {
                continue;
            }
            int required = Math.max(ingredient.getCount(), 1) * timesCrafted;
            for (int i = 0; i < sources.length && required > 0; i++) {
                if (sources[i] == null || available[i] <= 0 || !ingredient.match(sources[i].item())){
                    continue;
                }
                int taken = Math.min(required, available[i]);
                available[i] -= taken;
                required -= taken;
            }
            if (required > 0) {
                log.debug("Auto craft request does not consume enough items for ingredient {}", ingredient);
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static ConsumedSource resolveConsumedSource(Player player, ConsumeAction action) {
        try {
            var containerName = action.getSource().getFullContainerName();
            Inventory inventory = NetworkMapping.getInventory(player, containerName.getContainerName(), containerName.getDynamicID());
            if (inventory == null) {
                return null;
            }
            int slot = inventory.fromNetworkSlot(action.getSource().getSlot());
            if (slot < 0 || slot >= inventory.getSize()) {
                return null;
            }
            Item item = inventory.getUnclonedItem(slot);
            if (item == null || item.isNull()){
                return null;
            }
            return new ConsumedSource(inventory, slot, item);
        } catch (Throwable t) {
            log.debug("Failed to resolve the source item of an auto craft consume action", t);
            return null;
        }
    }

    private static List<ConsumeAction> findAllConsumeActions(ItemStackRequestAction[] actions, int startIndex) {
        var found = new ArrayList<ConsumeAction>();
        for (int i = startIndex; i < actions.length; i++) {
            var action = actions[i];
            if (action instanceof ConsumeAction consumeAction) {
                found.add(consumeAction);
            }
        }
        return found;
    }

     /**
     * The slot one consume action names, together with what that slot holds right now. Two actions
     * addressing the same container and slot compare equal through {@link #isSameSlotAs}.
     */
    record ConsumedSource(Inventory inventory, int slot, Item item){
        boolean isSameSlotAs(ConsumedSource other){
            return this.inventory == other.inventory && this.slot == other.slot;
        }
    }
}
