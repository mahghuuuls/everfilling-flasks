package com.mahghuuuls.everfillingflasks.flask;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stored-state contract on a real ItemStack: absent state reads as empty, writes round-trip
 * through NBT, and the two lifecycle writes (empty on equip, full on grant) leave exactly the
 * approved shape behind.
 */
class FlaskStackStateTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        // Registries must exist before an ItemStack can be constructed.
        Bootstrap.register();
    }

    private static ItemStack stack() {
        return new ItemStack(Items.APPLE);
    }

    @Test
    void aStackWithNoStateReadsAsZeroZero() {
        ItemStack s = stack();
        assertEquals(0, FlaskStackState.charges(s));
        assertEquals(0, FlaskStackState.progress(s));
    }

    @Test
    void writesRoundTrip() {
        ItemStack s = stack();
        FlaskStackState.setCharges(s, 3);
        FlaskStackState.setProgress(s, 542);
        assertEquals(3, FlaskStackState.charges(s));
        assertEquals(542, FlaskStackState.progress(s));
    }

    @Test
    void stateSurvivesNbtSerialization() {
        ItemStack s = stack();
        FlaskStackState.setCharges(s, 2);
        FlaskStackState.setProgress(s, 600);
        ItemStack reloaded = new ItemStack(s.serializeNBT());
        assertEquals(2, FlaskStackState.charges(reloaded));
        assertEquals(600, FlaskStackState.progress(reloaded));
    }

    @Test
    void emptyResetsBothValues() {
        ItemStack s = stack();
        FlaskStackState.initialiseFull(s, 4);
        FlaskStackState.setProgress(s, 100);
        FlaskStackState.empty(s);
        assertEquals(0, FlaskStackState.charges(s));
        assertEquals(0, FlaskStackState.progress(s));
    }

    @Test
    void initialiseFullSetsMaxChargesAndZeroProgress() {
        ItemStack s = stack();
        FlaskStackState.initialiseFull(s, 4);
        assertEquals(4, FlaskStackState.charges(s));
        assertEquals(0, FlaskStackState.progress(s));
    }

    @Test
    void stateLivesUnderTheModCompoundOnly() {
        ItemStack s = stack();
        FlaskStackState.setCharges(s, 1);
        assertNotNull(s.getTagCompound());
        // The approved layout: one compound under the mod id, nothing at the top level.
        assertTrue(s.getTagCompound().hasKey(FlaskStackState.TAG_ROOT));
        assertEquals(1, s.getTagCompound().getCompoundTag(FlaskStackState.TAG_ROOT)
                .getInteger(FlaskStackState.TAG_CHARGES));
    }
}
