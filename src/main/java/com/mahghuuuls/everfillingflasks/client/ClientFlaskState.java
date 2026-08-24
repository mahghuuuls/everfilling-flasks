package com.mahghuuuls.everfillingflasks.client;

import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import com.mahghuuuls.everfillingflasks.flask.FlaskMechanics;
import com.mahghuuuls.everfillingflasks.network.FlaskStateMessage;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The local player's mirror of the server's Flask state. Stores the last state message, counts
 * client ticks since it arrived, and builds read-only snapshots with interpolated recharge
 * progress, so the HUD moves smoothly between the server's one-per-second updates without any
 * client-side authority.
 */
@SideOnly(Side.CLIENT)
public final class ClientFlaskState {

    private static FlaskStateMessage last;
    private static int ticksSinceMessage;

    // Instantiable only for the event-bus registration in ClientProxy; state stays static.
    ClientFlaskState() {
    }

    public static void accept(FlaskStateMessage message) {
        // Runs on the main thread: the proxy marshals the netty callback through
        // addScheduledTask before it reaches here.
        ticksSinceMessage = 0;
        last = message;
    }

    /** Called every client tick from the proxy's registered handler. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && last != null) {
            ticksSinceMessage++;
        }
    }

    /** Leaving a server clears the mirror, so the next world never shows the last one's row. */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        last = null;
        ticksSinceMessage = 0;
    }

    /** The current read-only view. Without a server message yet: an empty snapshot. */
    public static FlaskSnapshot snapshot() {
        FlaskStateMessage message = last;
        if (message == null || !message.hasFlask()) {
            return new FlaskSnapshot(ItemStack.EMPTY, 0, 0, 0, 1, false, false, 0, 1, 0.0F);
        }
        int progress = FlaskMechanics.interpolateProgress(message.progressTicks(),
                ticksSinceMessage, message.rechargePaused(),
                message.charges() >= message.maxCharges(), message.rechargeTicks());
        // Drink progress interpolates the same way: the start message carried the duration,
        // and the server only speaks again at cancel or completion.
        int drinkProgress = message.drinking()
                ? Math.min(message.drinkProgressTicks() + ticksSinceMessage, message.drinkTicks())
                : 0;
        return new FlaskSnapshot(message.flask(), message.charges(),
                message.maxCharges(), progress, message.rechargeTicks(),
                message.rechargePaused(), message.drinking(), drinkProgress,
                message.drinkTicks(), message.hitThreshold());
    }
}
