package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import com.mahghuuuls.everfillingflasks.flask.IngredientRegistry;
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
 * server sends back): registered ingredients only, one item per slot. Every real write also
 * tells the player data the Flask's worth changed, so the effective cache refreshes and the
 * new potency numbers reach the client immediately.
 */
public final class IngredientGridHandler implements IItemHandlerModifiable {

    private final FlaskPlayerData data;

    public IngredientGridHandler(FlaskPlayerData data) {
        this.data = data;
    }

    private ItemStack flask() {
        ItemStack flask = data.equippedFlask();
        return FlaskRegistry.isFlask(flask) ? flask : ItemStack.EMPTY;
    }

    @Override
    public int getSlots() {
        return FlaskStackState.GRID_SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack flask = flask();
        return flask.isEmpty() ? ItemStack.EMPTY : FlaskStackState.ingredients(flask).get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack flask = flask();
        if (flask.isEmpty() || !IngredientRegistry.isIngredient(stack)) {
            return stack;
        }
        NonNullList<ItemStack> grid = FlaskStackState.ingredients(flask);
        if (!grid.get(slot).isEmpty()) {
            return stack;
        }
        ItemStack remainder = stack.copy();
        ItemStack piece = remainder.splitStack(1);
        if (!simulate) {
            grid.set(slot, piece);
            FlaskStackState.setIngredients(flask, grid);
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
        NonNullList<ItemStack> grid = FlaskStackState.ingredients(flask);
        ItemStack piece = grid.get(slot);
        if (piece.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            grid.set(slot, ItemStack.EMPTY);
            FlaskStackState.setIngredients(flask, grid);
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
        NonNullList<ItemStack> grid = FlaskStackState.ingredients(flask);
        if (stack.isEmpty()) {
            grid.set(slot, ItemStack.EMPTY);
        } else {
            ItemStack piece = stack.copy();
            piece.setCount(1);
            grid.set(slot, piece);
        }
        FlaskStackState.setIngredients(flask, grid);
        data.noteExternalChange();
    }

    @Override
    public int getSlotLimit(int slot) {
        // One item per grid slot, the owner's grid shape.
        return 1;
    }
}
