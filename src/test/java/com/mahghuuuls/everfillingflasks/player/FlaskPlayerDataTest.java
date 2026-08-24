package com.mahghuuuls.everfillingflasks.player;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The slot handler's storage contract: Flasks only, one at a time even through the shift-click
 * merge path, and no policy of its own. The handler must NOT change a stack's charges; the
 * equip rule lives in the container's server-side slot and is validated in the runtime
 * campaign, so any emptying observed here would be the old defect returning.
 */
class FlaskPlayerDataTest {

    private static Item flaskItem;

    @BeforeAll
    static void bootstrapAndRegisterAFlask() {
        Bootstrap.register();
        flaskItem = Items.GLASS_BOTTLE;
        // Refusal of a duplicate is fine: another test class may have used this item already.
        FlaskRegistry.register(flaskItem, new FlaskDefinition() {
            @Override
            public int maxCharges(ItemStack stack, EntityPlayer player) {
                return 4;
            }

            @Override
            public float healPercentage(ItemStack stack, EntityPlayer player) {
                return 0.3F;
            }

            @Override
            public int rechargeTicks(ItemStack stack, EntityPlayer player) {
                return 1200;
            }

            @Override
            public int drinkTicks(ItemStack stack, EntityPlayer player) {
                return 30;
            }

            @Override
            public float hitThreshold(ItemStack stack, EntityPlayer player) {
                return 1.0F;
            }
        });
    }

    private static ItemStack fullFlask() {
        ItemStack stack = new ItemStack(flaskItem);
        FlaskStackState.initialiseFull(stack, 4);
        return stack;
    }

    @Test
    void theSlotRefusesItemsThatAreNotFlasks() {
        FlaskPlayerData data = new FlaskPlayerData();
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStack remainder = data.slot().insertItem(0, sword, false);
        assertSame(sword, remainder);
        assertTrue(data.equippedFlask().isEmpty());
        assertFalse(data.slot().isItemValid(0, sword));
        assertTrue(data.slot().isItemValid(0, fullFlask()));
    }

    @Test
    void theHandlerStoresWithoutChangingCharges() {
        // Storage must be policy-free: the client's screen sync and Forge's slot probes write
        // through these methods, and neither is an equip.
        FlaskPlayerData data = new FlaskPlayerData();
        data.slot().setStackInSlot(0, fullFlask());
        assertEquals(4, FlaskStackState.charges(data.equippedFlask()));

        FlaskPlayerData other = new FlaskPlayerData();
        ItemStack remainder = other.slot().insertItem(0, fullFlask(), false);
        assertTrue(remainder.isEmpty());
        assertEquals(4, FlaskStackState.charges(other.equippedFlask()));
    }

    @Test
    void aRefusedInsertReturnsTheStackUntouched() {
        FlaskPlayerData data = new FlaskPlayerData();
        data.slot().setStackInSlot(0, fullFlask());
        ItemStack second = fullFlask();
        ItemStack remainder = data.slot().insertItem(0, second, false);
        // Slot occupied: nothing accepted, and the caller's stack keeps its charges.
        assertEquals(1, remainder.getCount());
        assertEquals(4, FlaskStackState.charges(remainder));
    }

    @Test
    void theSlotLimitIsOneWhereTheShiftClickMergeReadsIt() {
        // The merge's empty-slot branch fills up to Slot.getSlotStackLimit, which reads
        // the handler's getSlotLimit without the per-stack limit. Before the fix this was 64
        // and a command-created count-2 Flask stack could be shift-clicked in whole.
        FlaskPlayerData data = new FlaskPlayerData();
        assertEquals(1, data.slot().getSlotLimit(0));
        SlotItemHandler slot = new SlotItemHandler(data.slot(), 0, 0, 0);
        assertEquals(1, slot.getSlotStackLimit());
    }

    @Test
    void extractionKeepsTheStoredState() {
        FlaskPlayerData data = new FlaskPlayerData();
        data.slot().setStackInSlot(0, fullFlask());
        FlaskStackState.setCharges(data.equippedFlask(), 3);
        ItemStack taken = data.slot().extractItem(0, 1, false);
        assertEquals(3, FlaskStackState.charges(taken));
    }

    @Test
    void theForgeValidityProbeDoesNotCorruptTheStoredFlask() {
        // SlotItemHandler.isItemValid temporarily empties the slot and restores the original
        // stack through setStackInSlot. With any policy on that path the restore would corrupt
        // the stored charges.
        FlaskPlayerData data = new FlaskPlayerData();
        data.slot().setStackInSlot(0, fullFlask());
        SlotItemHandler slot = new SlotItemHandler(data.slot(), 0, 0, 0);
        slot.isItemValid(fullFlask());
        assertEquals(4, FlaskStackState.charges(data.equippedFlask()));
    }

    @Test
    void stateRoundTripsThroughNbt() {
        FlaskPlayerData data = new FlaskPlayerData();
        data.slot().setStackInSlot(0, fullFlask());
        data.markStartingFlaskGranted();

        FlaskPlayerData reloaded = new FlaskPlayerData();
        reloaded.deserializeNBT(data.serializeNBT());

        assertEquals(4, FlaskStackState.charges(reloaded.equippedFlask()));
        assertTrue(reloaded.startingFlaskGranted());
    }

    @Test
    void aFreshPlayerHasNoFlaskAndNoGrant() {
        FlaskPlayerData data = new FlaskPlayerData();
        assertTrue(data.equippedFlask().isEmpty());
        assertFalse(data.startingFlaskGranted());
    }
}
