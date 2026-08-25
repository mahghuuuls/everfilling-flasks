package com.mahghuuuls.everfillingflasks.client;

import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import com.mahghuuuls.everfillingflasks.network.FlaskStateMessage;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client mirror reproduces the server's numbers exactly at the moment a state message
 * lands: what the server put into the message is what a snapshot (and so a HUD replacement)
 * reads back. The server half of this equality — the message being built from the same values
 * as FlaskSnapshots.server — needs a live player and is checked in campaign C against the
 * displayed numbers.
 */
class ClientSnapshotMirrorTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void theMirrorSnapshotEqualsTheMessageJustAccepted() {
        FlaskStateMessage message = new FlaskStateMessage(true,
                new ItemStack(Items.GLASS_BOTTLE), 2, 4, 840, 1200, true, true, 12, 30,
                1.5F, 6, 10);
        ClientFlaskState.accept(message);

        FlaskSnapshot snapshot = ClientFlaskState.snapshot();
        assertTrue(snapshot.hasFlask());
        assertEquals(Items.GLASS_BOTTLE, snapshot.flask().getItem());
        assertEquals(2, snapshot.charges());
        assertEquals(4, snapshot.maxCharges());
        assertEquals(840, snapshot.rechargeProgressTicks(),
                "zero ticks have passed, so no interpolation may move the value");
        assertEquals(1200, snapshot.rechargeTicks());
        assertTrue(snapshot.rechargePaused());
        assertTrue(snapshot.drinking());
        assertEquals(12, snapshot.drinkProgressTicks());
        assertEquals(30, snapshot.drinkTicks());
        assertEquals(1.5F, snapshot.hitThreshold(), 1.0E-6F);
        assertEquals(6, ClientFlaskState.potencyUsed());
        assertEquals(10, ClientFlaskState.potency());
    }
}
