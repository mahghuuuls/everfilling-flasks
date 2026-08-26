package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The grid window's rebinding discipline: every call resolves the Flask in the slot right
 * then, so writes land on the live instance, a swap swaps the visible grid, and no Flask means
 * twelve refusing slots. Also the bounds: twelve slots exist, a Flask may have fewer, and the
 * ones it does not have must refuse rather than throw.
 */
class InfusionGridHandlerTest {

    private FlaskPlayerData data;
    private InfusionGridHandler handler;

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
        // Static registries shared with other test classes: register defensively, first wins.
        FlaskRegistry.register(Items.GLASS_BOTTLE, new NullFlask());
        com.mahghuuuls.everfillingflasks.flask.InfusionRegistry.register(Items.SUGAR,
                new SimpleInfusion());
    }

    @BeforeEach
    void freshData() {
        data = new FlaskPlayerData();
        handler = new InfusionGridHandler(data);
    }

    private static final class NullFlask implements FlaskDefinition {
        @Override
        public int maxCharges(ItemStack stack, EntityPlayer player) {
            return 1;
        }

        @Override
        public float healPercentage(ItemStack stack, EntityPlayer player) {
            return 0.0F;
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
    }

    private static final class SimpleInfusion implements InfusionDefinition {
        @Override
        public int potencyCost(ItemStack infusion) {
            return 2;
        }
    }

    private ItemStack equipFlask() {
        ItemStack flask = new ItemStack(Items.GLASS_BOTTLE);
        data.slot().setStackInSlot(0, flask);
        return flask;
    }

    @Test
    void withoutAFlaskEverySlotIsEmptyAndRefusing() {
        assertTrue(handler.getStackInSlot(0).isEmpty());
        ItemStack offered = new ItemStack(Items.SUGAR, 3);
        assertEquals(3, handler.insertItem(0, offered, false).getCount(),
                "no Flask, nothing accepted");
        assertTrue(handler.extractItem(0, 1, false).isEmpty());
    }

    @Test
    void insertTakesOnePieceAndWritesTheFlaskNbt() {
        ItemStack flask = equipFlask();
        ItemStack remainder = handler.insertItem(4, new ItemStack(Items.SUGAR, 3), false);

        assertEquals(2, remainder.getCount(), "one piece per insert");
        assertEquals(Items.SUGAR, FlaskStackState.infusions(flask).get(4).getItem(),
                "the write landed on the equipped Flask's own NBT");
        assertEquals(Items.SUGAR, handler.getStackInSlot(4).getItem());
    }

    @Test
    void anOccupiedSlotAndANonInfusionBothRefuse() {
        equipFlask();
        handler.insertItem(0, new ItemStack(Items.SUGAR), false);
        assertEquals(1, handler.insertItem(0, new ItemStack(Items.SUGAR), false).getCount(),
                "one item per slot");
        assertEquals(1, handler.insertItem(1, new ItemStack(Items.APPLE), false).getCount(),
                "unregistered items are refused");
    }

    @Test
    void simulatedInsertWritesNothing() {
        ItemStack flask = equipFlask();
        ItemStack remainder = handler.insertItem(0, new ItemStack(Items.SUGAR), true);
        assertTrue(remainder.isEmpty(), "the simulation reports acceptance");
        assertTrue(FlaskStackState.infusions(flask).get(0).isEmpty(),
                "but nothing was stored");
    }

    @Test
    void extractRemovesThePieceFromTheFlaskNbt() {
        ItemStack flask = equipFlask();
        handler.insertItem(2, new ItemStack(Items.SUGAR), false);

        ItemStack taken = handler.extractItem(2, 1, false);

        assertEquals(Items.SUGAR, taken.getItem());
        assertTrue(FlaskStackState.infusions(flask).get(2).isEmpty());
    }

    @Test
    void aFlaskSwapSwapsTheVisibleGridAndWritesNeverLandOnTheDeparted() {
        ItemStack first = equipFlask();
        handler.insertItem(0, new ItemStack(Items.SUGAR), false);

        ItemStack second = new ItemStack(Items.GLASS_BOTTLE);
        data.slot().setStackInSlot(0, second);

        assertTrue(handler.getStackInSlot(0).isEmpty(),
                "the new Flask's grid is what the slots show");
        handler.insertItem(1, new ItemStack(Items.SUGAR), false);
        assertTrue(FlaskStackState.infusions(first).get(1).isEmpty(),
                "the departed Flask was not written");
        assertEquals(Items.SUGAR, FlaskStackState.infusions(second).get(1).getItem());
        assertEquals(Items.SUGAR, FlaskStackState.infusions(first).get(0).getItem(),
                "the departed Flask kept its own infusions");
    }

    @Test
    void setStackClampsToOneItemAndClearsHonestly() {
        ItemStack flask = equipFlask();
        handler.setStackInSlot(3, new ItemStack(Items.SUGAR, 5));
        assertEquals(1, FlaskStackState.infusions(flask).get(3).getCount(),
                "the one-per-slot shape holds on the direct write path too");
        handler.setStackInSlot(3, ItemStack.EMPTY);
        assertTrue(FlaskStackState.infusions(flask).get(3).isEmpty());
    }

    @Test
    void aRealWriteMarksThePlayerDataDirty() {
        equipFlask();
        // The capability starts dirty; a fresh consume mimics the controller's send.
        dataSyncConsumed();
        handler.insertItem(0, new ItemStack(Items.SUGAR), false);
        assertTrue(dataSyncDirty(), "a grid edit must reach the client now, not on cadence");

        dataSyncConsumed();
        // An empty slot, so this exercises the simulated-accept path, not a refusal.
        handler.insertItem(5, new ItemStack(Items.SUGAR), true);
        assertFalse(dataSyncDirty(), "a simulation must not");
    }

    @Test
    void aDirectWriteWithNoFlaskIsDroppedAndPoisonsNothing() {
        // The departed-stack case of the putStack path: nowhere honest to write, so the write
        // is dropped — and never lands on the global empty-stack singleton's NBT.
        handler.setStackInSlot(0, new ItemStack(Items.SUGAR));
        assertTrue(handler.getStackInSlot(0).isEmpty());
        assertTrue(ItemStack.EMPTY.getTagCompound() == null,
                "the empty singleton must never gain NBT");
    }

    // The dirty flag is package-private in the player package; reflection keeps this test
    // honest without widening the field's access for production code.
    private void dataSyncConsumed() {
        setSyncDirty(false);
    }

    private boolean dataSyncDirty() {
        try {
            java.lang.reflect.Field field = FlaskPlayerData.class.getDeclaredField("syncDirty");
            field.setAccessible(true);
            return field.getBoolean(data);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private void setSyncDirty(boolean value) {
        try {
            java.lang.reflect.Field field = FlaskPlayerData.class.getDeclaredField("syncDirty");
            field.setAccessible(true);
            field.setBoolean(data, value);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }
    @Test
    void everySlotBeyondTheFlasksOwnCountRefusesInsteadOfThrowing() {
        // Twelve slots exist because a container's slot list is built once; a six-slot Flask
        // has to refuse the rest. Vanilla writes and reads every slot of a container without
        // asking whether it is enabled, so a slot that throws here takes the game down: the
        // client dies on the window-items packet, the server dies on a shift-click.
        data.slot().insertItem(0, new ItemStack(Items.GLASS_BOTTLE), false);

        assertEquals(12, handler.getSlots());
        assertEquals(6, handler.activeSlots());

        for (int slot = 6; slot < 12; slot++) {
            assertTrue(handler.getStackInSlot(slot).isEmpty(), "slot " + slot + " reads empty");
            assertEquals(1, handler.insertItem(slot, new ItemStack(Items.SUGAR), false).getCount(),
                    "slot " + slot + " refuses an infusion");
            assertTrue(handler.extractItem(slot, 1, false).isEmpty(),
                    "slot " + slot + " extracts nothing");
            handler.setStackInSlot(slot, new ItemStack(Items.SUGAR));
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Test
    void aNegativeSlotIsRefusedRatherThanTrusted() {
        data.slot().insertItem(0, new ItemStack(Items.GLASS_BOTTLE), false);

        assertTrue(handler.getStackInSlot(-1).isEmpty());
        assertTrue(handler.extractItem(-1, 1, false).isEmpty());
        handler.setStackInSlot(-1, new ItemStack(Items.SUGAR));
    }

    @Test
    void aPieceOutOfSightIsNotAPieceDestroyed() {
        // A Flask that shrank hides what no longer fits. Writing any slot it still has must not
        // take the hidden pieces with it, or a pack update would quietly eat a player's
        // infusions the first time they touched the grid.
        ItemStack flask = new ItemStack(Items.GLASS_BOTTLE);
        net.minecraft.util.NonNullList<ItemStack> twelve =
                net.minecraft.util.NonNullList.withSize(12, ItemStack.EMPTY);
        for (int i = 0; i < 12; i++) {
            twelve.set(i, new ItemStack(Items.SUGAR));
        }
        FlaskStackState.setInfusions(flask, twelve);

        // The Flask is a plain six-slot one, so a write covers only the first six.
        net.minecraft.util.NonNullList<ItemStack> six = FlaskStackState.infusions(flask, 6);
        six.set(0, ItemStack.EMPTY);
        FlaskStackState.setInfusions(flask, six);

        net.minecraft.util.NonNullList<ItemStack> read = FlaskStackState.infusions(flask, 12);
        assertTrue(read.get(0).isEmpty(), "the slot actually written is empty");
        for (int i = 6; i < 12; i++) {
            assertFalse(read.get(i).isEmpty(), "hidden slot " + i + " survives the write");
        }
    }

}
