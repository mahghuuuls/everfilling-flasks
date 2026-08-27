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
     * What a Flask has unless its own definition says otherwise. Five pieces at the usual cost
     * of two spend a potency budget of ten exactly, which is the owner's 2026-08-26 balance.
     */
    public static final int DEFAULT_GRID_SIZE = 5;

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
     * The infusion grid: as many slots as this Flask has, empty stacks for empty slots. A fresh
     * list every call; mutating it changes nothing until {@link #setInfusions}.
     *
     * <p>A stored piece in a slot the Flask no longer has is simply not read. It is not deleted
     * either: see {@link #setInfusions}.
     */
    public static NonNullList<ItemStack> infusions(ItemStack stack) {
        return infusions(stack, FlaskGrids.slots(stack));
    }

    /** The same, for callers that already know the count and must not ask twice. */
    public static NonNullList<ItemStack> infusions(ItemStack stack, int slots) {
        NonNullList<ItemStack> grid = NonNullList.withSize(slots, ItemStack.EMPTY);
        NBTTagCompound state = read(stack);
        if (state == null || !state.hasKey(TAG_INFUSIONS)) {
            return grid;
        }
        NBTTagList list = state.getTagList(TAG_INFUSIONS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getByte(TAG_SLOT);
            if (slot >= 0 && slot < slots) {
                grid.set(slot, new ItemStack(entry));
            }
        }
        return grid;
    }

    /**
     * Writes the slots this grid covers; only filled ones are stored.
     *
     * <p>Anything already stored beyond the grid handed in is left exactly as it was. A Flask
     * whose definition shrank stops showing the pieces that no longer fit, and this is what
     * stops the next click on any other slot from destroying them: they are out of sight, not
     * thrown away, and a Flask that grows back shows them again.
     */
    public static void setInfusions(ItemStack stack, NonNullList<ItemStack> grid) {
        int limit = Math.min(FlaskMechanics.MAX_INFUSION_SLOTS, grid.size());
        NBTTagList list = keptBeyond(stack, limit);
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

    /** The stored entries this write does not cover, carried over untouched. */
    private static NBTTagList keptBeyond(ItemStack stack, int limit) {
        NBTTagList kept = new NBTTagList();
        NBTTagCompound state = read(stack);
        if (state == null || !state.hasKey(TAG_INFUSIONS)) {
            return kept;
        }
        NBTTagList stored = state.getTagList(TAG_INFUSIONS, 10);
        for (int i = 0; i < stored.tagCount(); i++) {
            NBTTagCompound entry = stored.getCompoundTagAt(i);
            int slot = entry.getByte(TAG_SLOT);
            if (slot >= limit && slot < FlaskMechanics.MAX_INFUSION_SLOTS) {
                kept.appendTag(entry.copy());
            }
        }
        return kept;
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
