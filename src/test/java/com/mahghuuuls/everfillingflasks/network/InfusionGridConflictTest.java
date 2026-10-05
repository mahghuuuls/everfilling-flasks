package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import com.mahghuuuls.everfillingflasks.flask.InfusionRegistry;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerData;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The placement veto at the one point every player path consults (REQ-051) and the new
 * state-message field on the wire.
 */
class InfusionGridConflictTest {

    private FlaskPlayerData data;
    private InfusionGridHandler handler;
    private List<ItemStack> refusals;

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
        // Shared static registries: register defensively, first wins.
        FlaskRegistry.register(Items.GLASS_BOTTLE, new PlainFlask());
    }

    @BeforeEach
    void fresh() {
        // Items no other test class registers; first registration wins and behaves the same.
        InfusionRegistry.register(Items.GHAST_TEAR, new Refuses(Items.BLAZE_POWDER));
        InfusionRegistry.register(Items.BLAZE_POWDER, new Refuses(null));
        InfusionRegistry.register(Items.PRISMARINE_SHARD, new Refuses(null));
        data = new FlaskPlayerData();
        data.slot().setStackInSlot(0, new ItemStack(Items.GLASS_BOTTLE));
        handler = new InfusionGridHandler(data);
        refusals = new ArrayList<ItemStack>();
        handler.setRefusalListener(refusals::add);
    }

    private static final class PlainFlask implements FlaskDefinition {
        @Override public int maxCharges(ItemStack stack, EntityPlayer player) { return 1; }
        @Override public float healPercentage(ItemStack stack, EntityPlayer player) { return 0F; }
        @Override public int rechargeTicks(ItemStack stack, EntityPlayer player) { return 1200; }
        @Override public int drinkTicks(ItemStack stack, EntityPlayer player) { return 30; }
        @Override public float hitThreshold(ItemStack stack, EntityPlayer player) { return 1F; }
    }

    private static final class Refuses implements InfusionDefinition {
        private final net.minecraft.item.Item refuses;

        Refuses(net.minecraft.item.Item refuses) {
            this.refuses = refuses;
        }

        @Override
        public int potencyCost(ItemStack infusion) {
            return 1;
        }

        @Override
        public boolean conflictsWith(ItemStack self, ItemStack other) {
            return refuses != null && other.getItem() == refuses;
        }
    }

    @Test
    void aConflictingPieceIsRefusedAndTheGridIsUnchanged() {
        assertTrue(handler.insertItem(0, new ItemStack(Items.GHAST_TEAR), false).isEmpty());
        ItemStack brick = new ItemStack(Items.BLAZE_POWDER, 3);

        // Simulated, as a slot's validity check asks, then for real.
        ItemStack simulated = handler.insertItem(1, brick, true);
        ItemStack real = handler.insertItem(1, brick, false);

        assertEquals(3, simulated.getCount(), "nothing taken from the offered stack");
        assertEquals(3, real.getCount());
        assertTrue(handler.getStackInSlot(1).isEmpty(), "the grid is unchanged");
        assertEquals(Items.GHAST_TEAR, refusals.get(0).getItem(), "the placed piece is named");
    }

    @Test
    void theRefusalNamesThePieceAsItWasPlaced() {
        // A piece keeps its NBT through the grid, so an anvil name or an add-on's own data
        // survives placement; the refusal then names the piece as the player knows it.
        ItemStack named = new ItemStack(Items.GHAST_TEAR);
        named.setStackDisplayName("Concentrated Heartroot Infusion");
        assertTrue(handler.insertItem(0, named, false).isEmpty());
        assertEquals("Concentrated Heartroot Infusion", handler.getStackInSlot(0).getDisplayName());

        handler.insertItem(1, new ItemStack(Items.BLAZE_POWDER), true);

        assertEquals("Concentrated Heartroot Infusion", refusals.get(0).getDisplayName());
    }

    @Test
    void aNonConflictingPieceStillPlaces() {
        handler.insertItem(0, new ItemStack(Items.GHAST_TEAR), false);

        ItemStack remainder = handler.insertItem(1, new ItemStack(Items.PRISMARINE_SHARD), false);

        assertTrue(remainder.isEmpty());
        assertEquals(Items.PRISMARINE_SHARD, handler.getStackInSlot(1).getItem());
        assertTrue(refusals.isEmpty());
    }

    @Test
    void theMirrorWriteStaysUnchecked() {
        handler.insertItem(0, new ItemStack(Items.GHAST_TEAR), false);

        // The server-to-client mirror and the post-validation write: never vetoed, or the two
        // sides could disagree. The inert rule covers what arrives this way.
        handler.setStackInSlot(1, new ItemStack(Items.BLAZE_POWDER));

        assertEquals(Items.BLAZE_POWDER, handler.getStackInSlot(1).getItem());
        assertTrue(InfusionRegistry.hasConflict(
                FlaskStackState.infusions(data.equippedFlask())));
    }

    @Test
    void theConflictFlagSurvivesTheWire() {
        FlaskStateMessage sent = new FlaskStateMessage(true, new ItemStack(Items.GLASS_BOTTLE),
                1, 2, 0, 600, false, false, 0, 30, 1.0F, 2, 10, true);
        FlaskStateMessage received = new FlaskStateMessage();
        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        sent.toBytes(buf);
        received.fromBytes(buf);

        assertTrue(received.gridConflicted());
        assertEquals(2, received.potencyUsed());
        assertEquals(0, buf.readableBytes(), "every written byte must be consumed");
    }
}
