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

/** Wire round trips for the drink-visual broadcast. */
class DrinkVisualMessageTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void aStartVisualSurvivesTheRoundTrip() {
        DrinkVisualMessage sent =
                new DrinkVisualMessage(42, true, 30, new ItemStack(Items.GLASS_BOTTLE));
        DrinkVisualMessage received = new DrinkVisualMessage();
        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        sent.toBytes(buf);
        received.fromBytes(buf);

        assertEquals(42, received.entityId());
        assertTrue(received.drinking());
        assertEquals(30, received.drinkTicks());
        assertEquals(Items.GLASS_BOTTLE, received.flask().getItem());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void aStopVisualSurvivesTheRoundTrip() {
        DrinkVisualMessage received = new DrinkVisualMessage();
        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        new DrinkVisualMessage(42, false, 0, ItemStack.EMPTY).toBytes(buf);
        received.fromBytes(buf);

        assertFalse(received.drinking());
        assertTrue(received.flask().isEmpty());
        assertEquals(0, buf.readableBytes());
    }
}
