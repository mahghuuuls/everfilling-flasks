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

    /** Where the Flask slot draws: above the infusion row, aligned with its first slot. */
    public static final int FLASK_SLOT_X = 35;
    public static final int FLASK_SLOT_Y = 17;

    /**
     * Where the infusion row's first slot draws; six slots run right from here. 35 centers
     * the 108-wide row in the 176-wide panel.
     */
    public static final int GRID_X = 35;
    public static final int GRID_Y = 39;

    private static final int FLASK_SLOT_INDEX = 0;
    private static final int GRID_START = 1;
    private static final int GRID_END = GRID_START + FlaskStackState.GRID_SIZE;
    private static final int PLAYER_INVENTORY_START = GRID_END;
    private static final int HOTBAR_START = PLAYER_INVENTORY_START + 27;
    private static final int SLOT_COUNT = HOTBAR_START + 9;

    private final FlaskPlayerData data;
    private final IngredientGridHandler grid;

    public FlaskContainer(InventoryPlayer playerInventory, FlaskPlayerData data) {
        this.data = data;
        this.grid = new IngredientGridHandler(data);
        addSlotToContainer(new FlaskSlot(playerInventory.player, data.slot(),
                FLASK_SLOT_X, FLASK_SLOT_Y));
        // The grid slots sit over the stateless handler, so a Flask swap swaps their contents
        // and no slot ever remembers a departed stack. The handler's simulated insert and
        // extract are the validity and take checks; isEnabled hides the whole row, visually
        // and from clicks, while no Flask is equipped.
        for (int column = 0; column < FlaskStackState.GRID_SIZE; column++) {
            addSlotToContainer(new IngredientSlot(grid, column,
                    GRID_X + column * 18, GRID_Y));
        }
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
            // The merge copies the stack instead of moving the instance, so the live recharge
            // progress must be written into it first or the player receives a stale Flask.
            if (!player.world.isRemote) {
                data.flushLiveProgress();
                moved = slot.getStack();
                before = moved.copy();
            }
            if (!mergeItemStack(moved, PLAYER_INVENTORY_START, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            // The merge split the live stored stack, so the slot callbacks below only ever see
            // it already emptied; this is the one place the removal can be reported truthfully.
            if (!player.world.isRemote) {
                Diagnostics.slotChanged(player, before, ItemStack.EMPTY);
            }
        } else if (index < GRID_END) {
            // Out of the grid, into the player's inventory.
            if (!mergeItemStack(moved, PLAYER_INVENTORY_START, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // From the player's inventory: a Flask goes to the Flask slot; an ingredient goes
            // to the grid. The merge's empty-slot pass consults each slot's validity and limit,
            // so non-ingredients, a missing Flask, and the one-per-slot rule all refuse there.
            if (!mergeItemStack(moved, FLASK_SLOT_INDEX, FLASK_SLOT_INDEX + 1, false)
                    && !mergeItemStack(moved, GRID_START, GRID_END, false)) {
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

    /** Whether a Flask sits in the slot right now; the screen keys its drawing off this. */
    public boolean flaskEquipped() {
        return grid.hasFlask();
    }

    /**
     * One infusion slot: exists only while a Flask is equipped. Vanilla consults
     * {@code isEnabled} before drawing, hovering, or clicking a slot, so the empty screen
     * shows no grid at all; the handler's refusals stay as the server-side guard.
     */
    public static final class IngredientSlot extends SlotItemHandler {

        private final IngredientGridHandler grid;

        IngredientSlot(IngredientGridHandler grid, int index, int x, int y) {
            super(grid, index, x, y);
            this.grid = grid;
        }

        @Override
        public boolean isEnabled() {
            return grid.hasFlask();
        }
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
