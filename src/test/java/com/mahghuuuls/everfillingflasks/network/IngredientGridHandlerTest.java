package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.api.IngredientDefinition;
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
 * then, so writes land on the live instance, a swap swaps the visible grid, and no Flask
 * means nine refusing slots.
 */
class IngredientGridHandlerTest {

    private FlaskPlayerData data;
    private IngredientGridHandler handler;

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
        // Static registries shared with other test classes: register defensively, first wins.
        FlaskRegistry.register(Items.GLASS_BOTTLE, new NullFlask());
        com.mahghuuuls.everfillingflasks.flask.IngredientRegistry.register(Items.SUGAR,
                new SimpleIngredient());
    }

    @BeforeEach
    void freshData() {
        data = new FlaskPlayerData();
        handler = new IngredientGridHandler(data);
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

    private static final class SimpleIngredient implements IngredientDefinition {
        @Override
        public int potencyCost(ItemStack ingredient) {
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
        assertEquals(Items.SUGAR, FlaskStackState.ingredients(flask).get(4).getItem(),
                "the write landed on the equipped Flask's own NBT");
        assertEquals(Items.SUGAR, handler.getStackInSlot(4).getItem());
    }

    @Test
    void anOccupiedSlotAndANonIngredientBothRefuse() {
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
        assertTrue(FlaskStackState.ingredients(flask).get(0).isEmpty(),
                "but nothing was stored");
    }

    @Test
    void extractRemovesThePieceFromTheFlaskNbt() {
        ItemStack flask = equipFlask();
        handler.insertItem(2, new ItemStack(Items.SUGAR), false);

        ItemStack taken = handler.extractItem(2, 1, false);

        assertEquals(Items.SUGAR, taken.getItem());
        assertTrue(FlaskStackState.ingredients(flask).get(2).isEmpty());
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
        assertTrue(FlaskStackState.ingredients(first).get(1).isEmpty(),
                "the departed Flask was not written");
        assertEquals(Items.SUGAR, FlaskStackState.ingredients(second).get(1).getItem());
        assertEquals(Items.SUGAR, FlaskStackState.ingredients(first).get(0).getItem(),
                "the departed Flask kept its own ingredients");
    }

    @Test
    void setStackClampsToOneItemAndClearsHonestly() {
        ItemStack flask = equipFlask();
        handler.setStackInSlot(3, new ItemStack(Items.SUGAR, 5));
        assertEquals(1, FlaskStackState.ingredients(flask).get(3).getCount(),
                "the one-per-slot shape holds on the direct write path too");
        handler.setStackInSlot(3, ItemStack.EMPTY);
        assertTrue(FlaskStackState.ingredients(flask).get(3).isEmpty());
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
}
