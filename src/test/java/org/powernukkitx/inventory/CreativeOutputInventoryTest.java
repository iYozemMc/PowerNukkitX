package org.powernukkitx.inventory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.powernukkitx.ServerMockFixture;
import org.powernukkitx.item.Item;
import org.powernukkitx.item.ItemID;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The creative output is a one slot preview a station writes before the client takes from it. An
 * item stack request may name it before anything has been written, and the slot answers
 * {@code @NotNull}, so it has to hold air rather than nothing.
 */
class CreativeOutputInventoryTest {

    @BeforeAll
    static void boot() {
        ServerMockFixture.boot();
    }

    @Test
    void holdsAirBeforeAnythingIsWritten() {
        CreativeOutputInventory inventory = new CreativeOutputInventory(null);

        Item item = inventory.getItem(0);
        assertNotNull(item, "the output slot answered null");
        assertTrue(item.isNull(), "an untouched output slot is empty");
    }

    @Test
    void listsItsContentsBeforeAnythingIsWritten() {
        CreativeOutputInventory inventory = new CreativeOutputInventory(null);

        assertEquals(1, inventory.getContents().size());
        assertNotNull(inventory.getContents().get(0));
    }

    @Test
    void handsBackWhatWasWritten() {
        CreativeOutputInventory inventory = new CreativeOutputInventory(null);
        inventory.setItem(Item.get(ItemID.DIAMOND));

        assertEquals(ItemID.DIAMOND, inventory.getItem(0).getId());
    }

    @Test
    void goesBackToAirWhenCleared() {
        CreativeOutputInventory inventory = new CreativeOutputInventory(null);
        inventory.setItem(Item.get(ItemID.DIAMOND));
        inventory.clear(0, false);

        assertNotNull(inventory.getItem(0));
        assertTrue(inventory.getItem(0).isNull());
    }

    @Test
    void anEmptyContentsMapLeavesAirBehind() {
        CreativeOutputInventory inventory = new CreativeOutputInventory(null);
        inventory.setItem(Item.get(ItemID.DIAMOND));
        inventory.setContents(new HashMap<>());

        assertNotNull(inventory.getItem(0), "setContents without a slot 0 left the output slot null");
        assertTrue(inventory.getItem(0).isNull());
    }
}
