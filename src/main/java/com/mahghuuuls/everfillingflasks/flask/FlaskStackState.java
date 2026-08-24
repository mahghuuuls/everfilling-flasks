package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.Tags;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

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

    /** The equip rule: a Flask placed in the slot starts over. */
    public static void empty(ItemStack stack) {
        NBTTagCompound state = write(stack);
        state.setInteger(TAG_CHARGES, 0);
        state.setInteger(TAG_PROGRESS, 0);
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
