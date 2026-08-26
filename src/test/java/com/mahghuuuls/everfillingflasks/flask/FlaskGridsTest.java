package com.mahghuuuls.everfillingflasks.flask;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * How many infusion slots a Flask ends up with (REQ-045). The screen draws two rows of six, so
 * a definition asking for anything else has to be brought back inside that.
 */
class FlaskGridsTest {

    @Test
    @DisplayName("the range is one slot to twelve")
    void theRange() {
        assertEquals(1, FlaskMechanics.infusionSlots(1));
        assertEquals(12, FlaskMechanics.infusionSlots(12));
        assertEquals(6, FlaskMechanics.infusionSlots(6));
    }

    @Test
    @DisplayName("a definition asking for more than the screen can draw gets the ceiling")
    void tooMany() {
        assertEquals(12, FlaskMechanics.infusionSlots(13));
        assertEquals(12, FlaskMechanics.infusionSlots(100));
    }

    @Test
    @DisplayName("a Flask always has at least one slot, whatever it asks for")
    void tooFew() {
        // Zero would mean a Flask with a grid that cannot hold anything, which is a Flask with
        // no grid; the screen would draw an empty row and the player would wonder why.
        assertEquals(1, FlaskMechanics.infusionSlots(0));
        assertEquals(1, FlaskMechanics.infusionSlots(-5));
    }

    @Test
    @DisplayName("an unregistered stack answers with the default rather than nothing")
    void unregistered() {
        assertEquals(FlaskStackState.DEFAULT_GRID_SIZE, FlaskGrids.slots(ItemStack.EMPTY));
    }

    @Test
    @DisplayName("a definition that throws costs itself, not the grid")
    void throwingDefinition() {
        Item item = new Item();
        item.setRegistryName("everfillingflaskstest", "throwing_slots");
        FlaskRegistry.register(item, new FlaskDefinition() {
            @Override public int maxCharges(ItemStack stack, EntityPlayer player) { return 1; }
            @Override public float healPercentage(ItemStack stack, EntityPlayer player) { return 0.1F; }
            @Override public int rechargeTicks(ItemStack stack, EntityPlayer player) { return 20; }
            @Override public int drinkTicks(ItemStack stack, EntityPlayer player) { return 20; }
            @Override public float hitThreshold(ItemStack stack, EntityPlayer player) { return 1.0F; }
            @Override public int infusionSlots(ItemStack stack) {
                throw new IllegalStateException("test definition that always fails");
            }
        });

        assertEquals(FlaskStackState.DEFAULT_GRID_SIZE, FlaskGrids.slots(new ItemStack(item)));
    }
    @Test
    @DisplayName("what a definition actually asks for is what its Flask gets")
    void aDefinitionsOwnCountIsUsed() {
        assertEquals(3, slotsFor(3));
        assertEquals(12, slotsFor(12));
        assertEquals(1, slotsFor(1));
    }

    @Test
    @DisplayName("a definition asking for far too many is brought back to the ceiling")
    void anOverAskingDefinitionIsClamped() {
        // The clamp is tested on its own elsewhere; this proves the resolver actually applies
        // it, which a resolver that forgot to call it would still pass without.
        assertEquals(12, slotsFor(99));
        assertEquals(1, slotsFor(0));
    }

    private static int slotsFor(final int declared) {
        Item item = new Item();
        item.setRegistryName("everfillingflaskstest", "slots_" + declared);
        FlaskRegistry.register(item, new FlaskDefinition() {
            @Override public int maxCharges(ItemStack stack, EntityPlayer player) { return 1; }
            @Override public float healPercentage(ItemStack stack, EntityPlayer player) { return 0.1F; }
            @Override public int rechargeTicks(ItemStack stack, EntityPlayer player) { return 20; }
            @Override public int drinkTicks(ItemStack stack, EntityPlayer player) { return 20; }
            @Override public float hitThreshold(ItemStack stack, EntityPlayer player) { return 1.0F; }
            @Override public int infusionSlots(ItemStack stack) { return declared; }
        });
        return FlaskGrids.slots(new ItemStack(item));
    }

}
