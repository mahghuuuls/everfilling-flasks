package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.diagnostics.Diagnostics;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * The Flask screen's server-side container: the one Flask slot above the player's inventory.
 *
 * <p>The equip rule lives in {@link FlaskSlot#putStack}: every stack a player places into the
 * slot is emptied, on the server only. Vanilla routes each placement path that can put a Flask
 * here through {@code putStack} (direct clicks, swaps, and the shift-click merge; the one
 * bypass, the same-item top-up branch, cannot apply to a slot whose limit is one). The paths
 * that must not empty anything go around the rule: the client's screen synchronization also
 * calls {@code putStack} but is excluded by side, and Forge's slot-validity probes and the
 * starting-Flask grant write to the handler directly.
 */
public final class FlaskContainer extends Container {

    /** Where the Flask slot draws, the center slot of the placeholder background. */
    public static final int FLASK_SLOT_X = 80;
    public static final int FLASK_SLOT_Y = 35;

    private static final int FLASK_SLOT_INDEX = 0;
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int HOTBAR_START = PLAYER_INVENTORY_START + 27;
    private static final int SLOT_COUNT = HOTBAR_START + 9;

    public FlaskContainer(InventoryPlayer playerInventory, FlaskPlayerData data) {
        addSlotToContainer(new FlaskSlot(playerInventory.player, data.slot(),
                FLASK_SLOT_X, FLASK_SLOT_Y));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(playerInventory, 9 + row * 9 + column,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        // Player-bound, not block-bound: there is no position to walk away from.
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = slot.getStack();
        ItemStack before = moved.copy();
        if (index == FLASK_SLOT_INDEX) {
            if (!mergeItemStack(moved, PLAYER_INVENTORY_START, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            // The merge split the live stored stack, so the slot callbacks below only ever see
            // it already emptied; this is the one place the removal can be reported truthfully.
            if (!player.world.isRemote) {
                Diagnostics.slotChanged(player, before, ItemStack.EMPTY);
            }
        } else {
            if (!mergeItemStack(moved, FLASK_SLOT_INDEX, FLASK_SLOT_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (moved.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (moved.getCount() == before.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, moved);
        return before;
    }

    /** The Flask slot: the equip rule, and the diagnostics line, both server-side only. */
    public static final class FlaskSlot extends SlotItemHandler {

        private final EntityPlayer player;

        FlaskSlot(EntityPlayer player, IItemHandler handler, int x, int y) {
            super(handler, 0, x, y);
            this.player = player;
        }

        @Override
        public void putStack(ItemStack stack) {
            if (player.world.isRemote) {
                // The client's copy mirrors whatever the server sends; no rule applies here.
                super.putStack(stack);
                return;
            }
            ItemStack previous = getStack().copy();
            if (!stack.isEmpty()) {
                // The equip rule: a placed Flask starts over, so swapping cannot refill
                // mid-combat. Charges are only ever earned in the slot.
                FlaskStackState.empty(stack);
                super.putStack(stack);
                // Placements log here; removals log in onTake or the shift-click path, so one
                // action produces one line.
                Diagnostics.slotChanged(player, previous, stack);
                return;
            }
            super.putStack(stack);
        }

        @Override
        public ItemStack onTake(EntityPlayer taker, ItemStack taken) {
            // The shift-click merge empties the live stack before this runs; that path logs
            // from transferStackInSlot instead, and the empty `taken` keeps this quiet.
            if (!taker.world.isRemote && !taken.isEmpty()) {
                Diagnostics.slotChanged(taker, taken, getStack());
            }
            return super.onTake(taker, taken);
        }
    }
}
