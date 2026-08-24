package com.mahghuuuls.everfillingflasks.network;

import io.netty.buffer.Unpooled;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-format round trips for the state message. A field written and read in different orders,
 * or a forgotten field, shows up here instead of as a desynced HUD in game.
 */
class FlaskStateMessageTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void aFullStateSurvivesTheRoundTrip() {
        ItemStack flask = new ItemStack(Items.GLASS_BOTTLE);
        FlaskStateMessage sent =
                new FlaskStateMessage(true, flask, 2, 4, 840, 1200, true, true, 12, 30, 1.5F);
        FlaskStateMessage received = new FlaskStateMessage();
        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        sent.toBytes(buf);
        received.fromBytes(buf);

        assertTrue(received.hasFlask());
        assertEquals(Items.GLASS_BOTTLE, received.flask().getItem());
        assertEquals(2, received.charges());
        assertEquals(4, received.maxCharges());
        assertEquals(840, received.progressTicks());
        assertEquals(1200, received.rechargeTicks());
        assertTrue(received.rechargePaused());
        assertTrue(received.drinking());
        assertEquals(12, received.drinkProgressTicks());
        assertEquals(30, received.drinkTicks());
        assertEquals(1.5F, received.hitThreshold(), 1.0E-6F);
        assertEquals(0, buf.readableBytes(), "every written byte must be consumed");
    }

    @Test
    void theEmptyStateSurvivesTheRoundTrip() {
        FlaskStateMessage received = new FlaskStateMessage();
        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        FlaskStateMessage.empty().toBytes(buf);
        received.fromBytes(buf);

        assertFalse(received.hasFlask());
        assertTrue(received.flask().isEmpty());
        assertEquals(0, received.charges());
        assertEquals(0, buf.readableBytes());
    }
}
