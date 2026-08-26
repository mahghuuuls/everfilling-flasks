package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.flask.FlaskGrids;
import com.mahghuuuls.everfillingflasks.flask.FlaskMechanics;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import com.mahghuuuls.everfillingflasks.flask.InfusionRegistry;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerData;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * The screen's window onto the equipped Flask's infusion grid. Stateless on purpose: every
 * call resolves the Flask instance currently in the slot and goes through
 * {@link FlaskStackState}, which is what makes the ARC-010 rebinding rule hold by
 * construction — a write can never land on a departed stack, because nothing here ever holds
 * one. No equipped Flask means nine empty, refusing slots.
 *
 * <p>Rules enforced here, server and client alike (the client copy only mirrors what the
 * server sends back): registered infusions only, one item per slot. Every real write also
 * tells the player data the Flask's worth changed, so the effective cache refreshes and the
 * new potency numbers reach the client immediately.
 */
public final class InfusionGridHandler implements IItemHandlerModifiable {

    private final FlaskPlayerData data;

    public InfusionGridHandler(FlaskPlayerData data) {
        this.data = data;
    }

    private ItemStack flask() {
        ItemStack flask = data.equippedFlask();
        return FlaskRegistry.isFlask(flask) ? flask : ItemStack.EMPTY;
    }

    /** Whether a Flask is in the slot right now; the screen and the slots key off this. */
    public boolean hasFlask() {
        return !flask().isEmpty();
    }

    @Override
    public int getSlots() {
        // Fixed, because a container's slot list is built once and cannot grow. The Flask's own
        // count decides which of them accept anything; the rest simply refuse.
        return FlaskMechanics.MAX_INFUSION_SLOTS;
    }

    /** How many of those slots the equipped Flask actually has. */
    public int activeSlots() {
        ItemStack flask = flask();
        return flask.isEmpty() ? 0 : FlaskGrids.slots(flask);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack flask = flask();
        if (flask.isEmpty() || slot >= FlaskGrids.slots(flask)) {
            return ItemStack.EMPTY;
        }
        return FlaskStackState.infusions(flask).get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack flask = flask();
        if (flask.isEmpty() || slot >= FlaskGrids.slots(flask)
                || !InfusionRegistry.isInfusion(stack)) {
            return stack;
        }
        NonNullList<ItemStack> grid = FlaskStackState.infusions(flask);
        if (!grid.get(slot).isEmpty()) {
            return stack;
        }
        ItemStack remainder = stack.copy();
        ItemStack piece = remainder.splitStack(1);
        if (!simulate) {
            grid.set(slot, piece);
            FlaskStackState.setInfusions(flask, grid);
            data.noteExternalChange();
        }
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack flask = flask();
        if (flask.isEmpty()) {
            return ItemStack.EMPTY;
        }
        NonNullList<ItemStack> grid = FlaskStackState.infusions(flask);
        ItemStack piece = grid.get(slot);
        if (piece.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            grid.set(slot, ItemStack.EMPTY);
            FlaskStackState.setInfusions(flask, grid);
            data.noteExternalChange();
        }
        return piece.copy();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        // The container's putStack path: validity was already checked by the slot's simulated
        // insert, and the client's screen synchronization mirrors the server unchecked.
        ItemStack flask = flask();
        if (flask.isEmpty()) {
            // The Flask departed between the click and this write; there is nowhere honest to
            // put the item, and dropping the write is what keeps a departed stack untouched.
            return;
        }
        NonNullList<ItemStack> grid = FlaskStackState.infusions(flask);
        if (stack.isEmpty()) {
            grid.set(slot, ItemStack.EMPTY);
        } else {
            ItemStack piece = stack.copy();
            piece.setCount(1);
            grid.set(slot, piece);
        }
        FlaskStackState.setInfusions(flask, grid);
        data.noteExternalChange();
    }

    @Override
    public int getSlotLimit(int slot) {
        // One item per grid slot, the owner's grid shape.
        return 1;
    }
}
