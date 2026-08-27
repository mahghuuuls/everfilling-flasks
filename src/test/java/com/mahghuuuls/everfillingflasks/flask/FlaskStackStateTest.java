package com.mahghuuuls.everfillingflasks.flask;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
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
    void theGridRoundTripsThroughNbtWithSlotsPreserved() {
        ItemStack s = stack();
        net.minecraft.util.NonNullList<ItemStack> grid =
                net.minecraft.util.NonNullList.withSize(FlaskStackState.DEFAULT_GRID_SIZE,
                        ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));
        grid.set(3, new ItemStack(Items.BLAZE_POWDER));
        grid.set(4, new ItemStack(Items.SUGAR));
        FlaskStackState.setInfusions(s, grid);

        ItemStack reloaded = new ItemStack(s.serializeNBT());
        net.minecraft.util.NonNullList<ItemStack> read = FlaskStackState.infusions(reloaded);
        assertEquals(FlaskStackState.DEFAULT_GRID_SIZE, read.size());
        assertEquals(Items.SUGAR, read.get(0).getItem());
        assertTrue(read.get(1).isEmpty());
        assertEquals(Items.BLAZE_POWDER, read.get(3).getItem());
        assertEquals(Items.SUGAR, read.get(4).getItem());
    }

    @Test
    void aStoredSlotPastTheGridIsDroppedOnRead() {
        // The nine-to-six revision: an old stored slot index at or past six must vanish
        // silently, not shift or crash.
        ItemStack s = stack();
        net.minecraft.nbt.NBTTagCompound entry = new net.minecraft.nbt.NBTTagCompound();
        entry.setByte("slot", (byte) 8);
        new ItemStack(Items.SUGAR).writeToNBT(entry);
        net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
        list.appendTag(entry);
        net.minecraft.nbt.NBTTagCompound root = new net.minecraft.nbt.NBTTagCompound();
        root.setTag(FlaskStackState.TAG_INFUSIONS, list);
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setTag(FlaskStackState.TAG_ROOT, root);
        s.setTagCompound(tag);

        net.minecraft.util.NonNullList<ItemStack> read = FlaskStackState.infusions(s);
        assertEquals(FlaskStackState.DEFAULT_GRID_SIZE, read.size());
        for (ItemStack piece : read) {
            assertTrue(piece.isEmpty());
        }
    }

    @Test
    void aStackWithNoGridReadsAsAllEmptySlots() {
        net.minecraft.util.NonNullList<ItemStack> read = FlaskStackState.infusions(stack());
        assertEquals(FlaskStackState.DEFAULT_GRID_SIZE, read.size());
        for (ItemStack piece : read) {
            assertTrue(piece.isEmpty());
        }
    }

    @Test
    void theEquipEmptyLeavesTheGridUntouched() {
        // The owner's rule: moving a Flask costs its charges, never its infusions.
        ItemStack s = stack();
        net.minecraft.util.NonNullList<ItemStack> grid =
                net.minecraft.util.NonNullList.withSize(FlaskStackState.DEFAULT_GRID_SIZE,
                        ItemStack.EMPTY);
        grid.set(3, new ItemStack(Items.SUGAR));
        FlaskStackState.setInfusions(s, grid);
        FlaskStackState.initialiseFull(s, 4);

        FlaskStackState.empty(s);

        assertEquals(0, FlaskStackState.charges(s));
        assertEquals(Items.SUGAR, FlaskStackState.infusions(s).get(3).getItem());
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
    @Test
    void aFlaskThatShrankKeepsOnlyWhatStillFits() {
        // A pack changes a Flask's definition, or an add-on ships a smaller Flask than the one
        // a player already filled. What no longer fits is gone; what fits is untouched.
        ItemStack flask = new ItemStack(Items.GLASS_BOTTLE);
        NonNullList<ItemStack> six = NonNullList.withSize(6, ItemStack.EMPTY);
        for (int i = 0; i < 6; i++) {
            six.set(i, new ItemStack(Items.WHEAT_SEEDS, 1));
        }
        FlaskStackState.setInfusions(flask, six);

        NonNullList<ItemStack> read = FlaskStackState.infusions(flask, 3);

        assertEquals(3, read.size());
        for (int i = 0; i < 3; i++) {
            org.junit.jupiter.api.Assertions.assertFalse(read.get(i).isEmpty(),
                    "slot " + i + " still fits and must survive");
        }
    }

    @Test
    void aGridIsReadAtTheSizeItIsAskedFor() {
        ItemStack flask = new ItemStack(Items.GLASS_BOTTLE);
        assertEquals(12, FlaskStackState.infusions(flask, 12).size());
        assertEquals(1, FlaskStackState.infusions(flask, 1).size());
    }

}
