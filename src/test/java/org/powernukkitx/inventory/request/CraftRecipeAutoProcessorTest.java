package org.powernukkitx.inventory.request;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.powernukkitx.ServerMockFixture;
import org.powernukkitx.inventory.BaseInventory;
import org.powernukkitx.inventory.Inventory;
import org.powernukkitx.item.Item;
import org.powernukkitx.item.ItemID;
import org.powernukkitx.recipe.descriptor.DefaultDescriptor;
import org.powernukkitx.recipe.descriptor.ItemDescriptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An auto craft is paid for by the consume actions the client appends to the request, and the
 * client picks both how many there are and which slot each one names. The coverage check is what
 * keeps a request from taking an output it did not pay the ingredients for.
 */
class CraftRecipeAutoProcessorTest {

    @BeforeAll
    static void boot() {
        ServerMockFixture.boot();
    }

    private static Inventory grid() {
        return new BaseInventory(null, ContainerType.CONTAINER, 9) {
        };
    }

    /** A recipe needing {@code count} diamonds for one craft. */
    private static List<ItemDescriptor> diamondsNeeded(int count) {
        return List.of(new DefaultDescriptor(Item.get(ItemID.DIAMOND, 0, count)));
    }

    private static CraftRecipeAutoProcessor.ConsumedSource source(Inventory inventory, int slot, int count) {
        return new CraftRecipeAutoProcessor.ConsumedSource(inventory, slot, Item.get(ItemID.DIAMOND, 0, count));
    }

    @Test
    void oneSlotPerIngredientCoversTheRecipe() {
        Inventory grid = grid();

        assertTrue(CraftRecipeAutoProcessor.consumeActionsCoverRecipe(
            diamondsNeeded(2),
            new CraftRecipeAutoProcessor.ConsumedSource[]{source(grid, 0, 1), source(grid, 1, 1)},
            new int[]{1, 1},
            1));
    }

    @Test
    void aSingleSlotHoldingEnoughCoversTheRecipe() {
        Inventory grid = grid();

        assertTrue(CraftRecipeAutoProcessor.consumeActionsCoverRecipe(
            diamondsNeeded(2),
            new CraftRecipeAutoProcessor.ConsumedSource[]{source(grid, 0, 2)},
            new int[]{2},
            1));
    }

    /**
     * The one that mattered: both actions name slot 0, which holds a single diamond, so between
     * them they can only ever take that one diamond.
     */
    @Test
    void twoActionsOnOneSlotDoNotPayTwice() {
        Inventory grid = grid();
        CraftRecipeAutoProcessor.ConsumedSource slotZero = source(grid, 0, 1);

        assertFalse(CraftRecipeAutoProcessor.consumeActionsCoverRecipe(
            diamondsNeeded(2),
            new CraftRecipeAutoProcessor.ConsumedSource[]{slotZero, source(grid, 0, 1)},
            new int[]{1, 1},
            1), "the same slot was credited twice");
    }

    @Test
    void theSameSlotOfTwoContainersIsStillTwoSlots() {
        Inventory grid = grid();
        Inventory chest = grid();

        assertTrue(CraftRecipeAutoProcessor.consumeActionsCoverRecipe(
            diamondsNeeded(2),
            new CraftRecipeAutoProcessor.ConsumedSource[]{source(grid, 0, 1), source(chest, 0, 1)},
            new int[]{1, 1},
            1));
    }

    @Test
    void anActionCannotClaimMoreThanItsSlotHolds() {
        Inventory grid = grid();

        assertFalse(CraftRecipeAutoProcessor.consumeActionsCoverRecipe(
            diamondsNeeded(2),
            new CraftRecipeAutoProcessor.ConsumedSource[]{source(grid, 0, 1)},
            new int[]{64},
            1));
    }

    @Test
    void anUnresolvedActionPaysNothing() {
        assertFalse(CraftRecipeAutoProcessor.consumeActionsCoverRecipe(
            diamondsNeeded(1),
            new CraftRecipeAutoProcessor.ConsumedSource[]{null},
            new int[]{1},
            1));
    }

    @Test
    void craftingSeveralTimesNeedsTheIngredientsSeveralTimes() {
        Inventory grid = grid();
        CraftRecipeAutoProcessor.ConsumedSource[] sources =
            new CraftRecipeAutoProcessor.ConsumedSource[]{source(grid, 0, 4)};

        assertTrue(CraftRecipeAutoProcessor.consumeActionsCoverRecipe(diamondsNeeded(2), sources, new int[]{4}, 2));
        assertFalse(CraftRecipeAutoProcessor.consumeActionsCoverRecipe(diamondsNeeded(2), sources, new int[]{4}, 3));
    }
}
