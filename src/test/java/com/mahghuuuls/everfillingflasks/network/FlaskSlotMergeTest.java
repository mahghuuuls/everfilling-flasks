package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduction of a shift-click into the empty Flask slot that appeared to do nothing in a dev
 * client: vanilla's {@code Container.mergeItemStack} empty-slot branch asks
 * {@code SlotItemHandler.isItemValid}, then places via {@code Slot.putStack}. This test performs
 * exactly those calls against the real handler, in order, so whichever step refuses is named by
 * a failing assertion instead of a silent no-op in game.
 */
class FlaskSlotMergeTest {

    private static Item flaskItem;

    @BeforeAll
    static void bootstrapAndRegisterAFlask() {
        Bootstrap.register();
        flaskItem = Items.DRAGON_BREATH;
        FlaskRegistry.register(flaskItem, new FlaskDefinition() {
            @Override
            public int maxCharges(ItemStack stack, EntityPlayer player) {
                return 4;
            }

            @Override
            public float healPercentage(ItemStack stack, EntityPlayer player) {
                return 0.3F;
            }

            @Override
            public int rechargeTicks(ItemStack stack, EntityPlayer player) {
                return 1200;
            }

            @Override
            public int drinkTicks(ItemStack stack, EntityPlayer player) {
                return 30;
            }

            @Override
            public float hitThreshold(ItemStack stack, EntityPlayer player) {
                return 1.0F;
            }
        });
    }

    @Test
    void theMergePathAcceptsAFlaskIntoTheEmptySlot() {
        FlaskPlayerData data = new FlaskPlayerData();
        // Plain SlotItemHandler: FlaskSlot only changes putStack/onTake behavior on a live
        // server world; the validity and limit reads under test are inherited unchanged.
        SlotItemHandler slot = new SlotItemHandler(data.slot(), 0, 0, 0);

        ItemStack moved = new ItemStack(flaskItem);
        FlaskStackState.initialiseFull(moved, 4);

        // Container.mergeItemStack, empty-slot branch, exact call order:
        assertTrue(slot.getStack().isEmpty(), "precondition: slot empty");
        assertTrue(slot.isItemValid(moved), "SlotItemHandler.isItemValid must accept a Flask");
        assertEquals(1, slot.getSlotStackLimit(), "merge fills up to getSlotStackLimit");
        ItemStack split = moved.splitStack(Math.min(moved.getCount(), slot.getSlotStackLimit()));
        slot.putStack(split);

        assertEquals(1, data.slot().getStackInSlot(0).getCount(),
                "the Flask must be stored after putStack");
        assertTrue(moved.isEmpty(), "the source stack must be consumed");
    }
}
