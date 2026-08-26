package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.Tags;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;

/**
 * The only class that touches a Flask stack's stored state.
 *
 * <p>Layout, an architectural contract: one compound under the mod id with {@code charges} and
 * {@code progress}. A stack with no stored state reads as 0 charges and 0 progress, because a
 * Flask gains charges only in the Flask slot; the starting Flask is created full explicitly.
 *
 * <p>These are reads and writes, not rules. When to write, and what the values mean, belongs
 * to the drink controller.
 */
public final class FlaskStackState {

    static final String TAG_ROOT = Tags.MOD_ID;
    static final String TAG_CHARGES = "charges";
    static final String TAG_PROGRESS = "progress";
    static final String TAG_INFUSIONS = "infusions";
    private static final String TAG_SLOT = "slot";

    /**
     * The infusion grid's size: one row of six, the owner's 2026-08-25 revision (a
     * three-by-three read as a crafting table). Stored slot indexes at or past this are
     * dropped on read, which retires any dev-world grid piece from the nine-slot days.
     */
    public static final int GRID_SIZE = 6;

    private FlaskStackState() {
    }

    /** Stored charges; 0 for a stack with no state. */
    public static int charges(ItemStack stack) {
        NBTTagCompound state = read(stack);
        return state == null ? 0 : state.getInteger(TAG_CHARGES);
    }

    /** Stored recharge progress in ticks; 0 for a stack with no state. */
    public static int progress(ItemStack stack) {
        NBTTagCompound state = read(stack);
        return state == null ? 0 : state.getInteger(TAG_PROGRESS);
    }

    public static void setCharges(ItemStack stack, int charges) {
        write(stack).setInteger(TAG_CHARGES, charges);
    }

    public static void setProgress(ItemStack stack, int progress) {
        write(stack).setInteger(TAG_PROGRESS, progress);
    }

    /**
     * The equip rule: a Flask placed in the slot starts over. Charges and progress only; the
     * infusion grid rides along untouched, by the owner's rule that moving a Flask never costs
     * its infusions.
     */
    public static void empty(ItemStack stack) {
        NBTTagCompound state = write(stack);
        state.setInteger(TAG_CHARGES, 0);
        state.setInteger(TAG_PROGRESS, 0);
    }

    /**
     * The infusion grid: always {@link #GRID_SIZE} slots, empty stacks for empty slots. A
     * fresh list every call; mutating it changes nothing until {@link #setInfusions}.
     */
    public static NonNullList<ItemStack> infusions(ItemStack stack) {
        NonNullList<ItemStack> grid = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        NBTTagCompound state = read(stack);
        if (state == null || !state.hasKey(TAG_INFUSIONS)) {
            return grid;
        }
        NBTTagList list = state.getTagList(TAG_INFUSIONS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getByte(TAG_SLOT);
            if (slot >= 0 && slot < GRID_SIZE) {
                grid.set(slot, new ItemStack(entry));
            }
        }
        return grid;
    }

    /** Writes the whole grid; only filled slots are stored. Oversized lists are truncated. */
    public static void setInfusions(ItemStack stack, NonNullList<ItemStack> grid) {
        NBTTagList list = new NBTTagList();
        int limit = Math.min(GRID_SIZE, grid.size());
        for (int i = 0; i < limit; i++) {
            ItemStack piece = grid.get(i);
            if (!piece.isEmpty()) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setByte(TAG_SLOT, (byte) i);
                piece.writeToNBT(entry);
                list.appendTag(entry);
            }
        }
        write(stack).setTag(TAG_INFUSIONS, list);
    }

    /** Used only by the starting-Flask grant, which is the one full-by-creation path. */
    public static void initialiseFull(ItemStack stack, int maxCharges) {
        NBTTagCompound state = write(stack);
        state.setInteger(TAG_CHARGES, maxCharges);
        state.setInteger(TAG_PROGRESS, 0);
    }

    private static NBTTagCompound read(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_ROOT)) {
            return null;
        }
        return tag.getCompoundTag(TAG_ROOT);
    }

    private static NBTTagCompound write(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (!tag.hasKey(TAG_ROOT)) {
            tag.setTag(TAG_ROOT, new NBTTagCompound());
        }
        return tag.getCompoundTag(TAG_ROOT);
    }
}
