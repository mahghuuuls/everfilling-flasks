package com.mahghuuuls.everfillingflasks.player;

import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Everything Flask-shaped that belongs to one player: the Flask slot, the once-only
 * starting-Flask flag, and the transient recharge and drinking bookkeeping the controller
 * keeps between ticks.
 *
 * <p>NBT layout, an architectural contract: {@code { slot: <ItemStackHandler NBT>, granted:
 * byte }}.
 */
public final class FlaskPlayerData {

    private final FlaskSlotHandler slot = new FlaskSlotHandler();
    private boolean startingFlaskGranted;

    // Server-side recharge bookkeeping, owned by DrinkController and deliberately transient:
    // live progress ticks between NBT flushes, the stack instance being tracked so a slot change
    // is detected by reference, and the flush/sync clocks. None of it persists; a reload starts
    // from the stack's stored NBT. Clocks start at zero, not Long.MIN_VALUE: the cadence
    // predicates treat a clock ahead of the world time as due, so no sentinel arithmetic can
    // overflow into a never-firing comparison.
    ItemStack trackedStack = ItemStack.EMPTY;
    com.mahghuuuls.everfillingflasks.flask.EffectiveFlask cachedEffective;
    int liveProgress;
    boolean liveValid;
    boolean rechargePaused;
    boolean syncDirty = true;
    long lastFlushTick;
    long lastSyncTick;
    long lastEffectiveRefreshTick;

    // Transient drinking state, owned by DrinkController and never persisted: a restart
    // means idle. The effective values are frozen at drink start so a mid-drink modifier change
    // cannot stretch or shorten a drink already committed to.
    boolean drinking;
    int drinkElapsed;
    com.mahghuuuls.everfillingflasks.flask.EffectiveFlask drinkEffective;
    ItemStack drinkStack = ItemStack.EMPTY;

    /** Whether this player is mid-drink; the guards and renderers key off this alone. */
    public boolean drinking() {
        return drinking;
    }

    /**
     * Writes the live recharge progress into the equipped stack's NBT right now. Callers are
     * the moments the stack's stored state is about to be read or copied by someone else:
     * logout, the death/End clone, and the shift-click merge, which copies the stack rather
     * than moving the instance.
     */
    public void flushLiveProgress() {
        // Into the tracked stack, never the slot's current content: between a container click
        // and the next tick's reconciliation the two can differ, and writing the live value of
        // one Flask into another would duplicate recharge progress.
        if (liveValid && !trackedStack.isEmpty()) {
            FlaskStackState.setProgress(trackedStack, liveProgress);
        }
    }

    public FlaskSlotHandler slot() {
        return slot;
    }

    /**
     * Something outside the controller changed what the equipped Flask is worth — today the
     * infusion grid being edited in the screen. The cached effective values are stale and the
     * owner should hear about it now, not on the one-second cadence. Harmless on a client-side
     * copy, where neither field is ever read.
     */
    public void noteExternalChange() {
        cachedEffective = null;
        syncDirty = true;
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
