package com.mahghuuuls.everfillingflasks.client;

import com.mahghuuuls.everfillingflasks.network.FlaskKeyMessage;
import com.mahghuuuls.everfillingflasks.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Watches the Use Flask key and reports its edges to the server.
 *
 * <p>The key counts as up whenever any screen is open or the player is gone, so opening the
 * inventory, chat, or a menu mid-drink sends the release that cancels it, and losing window
 * focus does the same. No drinking decision is made here.
 */
@SideOnly(Side.CLIENT)
public final class FlaskKeyHandler {

    private boolean lastSent;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        boolean screenOpen = mc.currentScreen != null;
        boolean held = mc.player != null && !screenOpen && FlaskKeys.USE_FLASK.isKeyDown();
        if (held != lastSent) {
            lastSent = held;
            byte cause = !held && screenOpen
                    ? FlaskKeyMessage.RELEASE_SCREEN : FlaskKeyMessage.RELEASE_KEY;
            PacketHandler.CHANNEL.sendToServer(new FlaskKeyMessage(held, cause));
        }
    }
}
