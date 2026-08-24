package com.mahghuuuls.everfillingflasks.player;

import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Everything Flask-shaped that belongs to one player: the Flask slot, and the once-only
 * starting-Flask flag. Transient drinking state joins this class in a later slice.
 *
 * <p>NBT layout, an architectural contract: {@code { slot: <ItemStackHandler NBT>, granted:
 * byte }}.
 */
public final class FlaskPlayerData {

    private final FlaskSlotHandler slot = new FlaskSlotHandler();
    private boolean startingFlaskGranted;

    public FlaskSlotHandler slot() {
        return slot;
    }

    public ItemStack equippedFlask() {
        return slot.getStackInSlot(0);
    }

    public boolean startingFlaskGranted() {
        return startingFlaskGranted;
    }

    public void markStartingFlaskGranted() {
        startingFlaskGranted = true;
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("slot", slot.serializeNBT());
        tag.setBoolean("granted", startingFlaskGranted);
        return tag;
    }

    public void deserializeNBT(NBTTagCompound tag) {
        slot.deserializeNBT(tag.getCompoundTag("slot"));
        startingFlaskGranted = tag.getBoolean("granted");
    }

    /**
     * The Flask slot: plain storage that only accepts Flasks, one at a time. Deliberately no
     * policy here: the equip rule that empties a placed Flask belongs to the server-side
     * container slot, because this handler is also written by Forge's slot probes and by the
     * client's screen synchronization, and neither of those is an equip.
     */
    public static final class FlaskSlotHandler extends ItemStackHandler {

        FlaskSlotHandler() {
            super(1);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return FlaskRegistry.isFlask(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            // One Flask. The shift-click merge's empty-slot branch fills up to this limit
            // without consulting the per-stack limit, so it must be enforced here.
            return 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // ItemStackHandler does not consult isItemValid on its own in this Forge version.
            if (!stack.isEmpty() && !isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }
    }
}
