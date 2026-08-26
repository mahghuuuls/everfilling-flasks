package com.mahghuuuls.everfillingflasks.client;

import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import com.mahghuuuls.everfillingflasks.flask.FlaskMechanics;
import com.mahghuuuls.everfillingflasks.network.DrinkVisualMessage;
import com.mahghuuuls.everfillingflasks.network.FlaskStateMessage;
import net.minecraft.entity.player.EntityPlayer;
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

    /** Past this age the interrupted look is long gone; also the "never interrupted" value. */
    private static final int INTERRUPT_AGE_CAP = 1000;

    /**
     * The local player's last drink interruption, for the cast bar's fading interrupted look.
     * The outcome rides the visual message, which the server sends before the state message
     * in the same tick, so it is pending here until the mirror confirms the drink ended.
     */
    private static byte pendingOutcome = DrinkVisualMessage.OUTCOME_NONE;
    private static float interruptedFill;
    private static int ticksSinceInterrupt = INTERRUPT_AGE_CAP;

    /**
     * Other players' drink visuals from the broadcast, keyed by entity id, with a lifetime so a
     * lost stop message cannot leave someone drinking forever: entries die on their own once the
     * announced duration is well past.
     */
    private static final java.util.Map<Integer, Visual> VISUALS =
            new java.util.HashMap<Integer, Visual>();

    private static final class Visual {
        final ItemStack flask;
        final int drinkTicks;
        int age;

        Visual(ItemStack flask, int drinkTicks) {
            this.flask = flask;
            this.drinkTicks = drinkTicks;
        }
    }

    // Instantiable only for the event-bus registration in ClientProxy; state stays static.
    ClientFlaskState() {
    }

    public static void accept(FlaskStateMessage message) {
        // Runs on the main thread: the proxy marshals the netty callback through
        // addScheduledTask before it reaches here.
        if (last != null && last.drinking() && !message.drinking()
                && pendingOutcome == DrinkVisualMessage.OUTCOME_INTERRUPTED) {
            // The bar freezes at the fill the player last saw, then fades from there.
            FlaskSnapshot ended = snapshot();
            interruptedFill = com.mahghuuuls.everfillingflasks.client.hud.CastBar.fill(
                    ended.drinkProgressTicks(), 0.0F, ended.drinkTicks());
            ticksSinceInterrupt = 0;
        }
        pendingOutcome = DrinkVisualMessage.OUTCOME_NONE;
        ticksSinceMessage = 0;
        last = message;
    }

    /** A drink-visual broadcast landing; main thread via the proxy. */
    public static void acceptVisual(DrinkVisualMessage message) {
        net.minecraft.client.entity.EntityPlayerSP self =
                net.minecraft.client.Minecraft.getMinecraft().player;
        if (!message.drinking() && self != null && message.entityId() == self.getEntityId()) {
            pendingOutcome = message.outcome();
        }
        if (message.drinking() && !message.flask().isEmpty()) {
            VISUALS.put(message.entityId(), new Visual(message.flask(), message.drinkTicks()));
        } else {
            VISUALS.remove(message.entityId());
        }
    }

    /** The server's summed infusion costs for the equipped Flask; display only. */
    public static int potencyUsed() {
        return last == null ? 0 : last.potencyUsed();
    }

    /** The server's potency budget for the equipped Flask; display only. */
    public static int potency() {
        return last == null ? 0 : last.potency();
    }

    /** Client ticks since the local player's last interruption; large when none is recent. */
    public static int ticksSinceInterrupt() {
        return ticksSinceInterrupt;
    }

    /** The fill fraction the interrupted drink froze at. Meaningful only while fading. */
    public static float interruptedFill() {
        return interruptedFill;
    }

    /** Whether this player should be drawn drinking, local mirror or broadcast. */
    public static boolean isDrinkingVisually(EntityPlayer player) {
        return !visualFlask(player).isEmpty();
    }

    /** The Flask to draw in this player's hand, or empty when they are not drawn drinking. */
    public static ItemStack visualFlask(EntityPlayer player) {
        if (net.minecraft.client.Minecraft.getMinecraft().player == player) {
            FlaskSnapshot state = snapshot();
            return state.drinking() ? state.flask() : ItemStack.EMPTY;
        }
        Visual visual = VISUALS.get(player.getEntityId());
        return visual == null ? ItemStack.EMPTY : visual.flask;
    }

    /** Called every client tick from the proxy's registered handler. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (last != null) {
            ticksSinceMessage++;
        }
        if (ticksSinceInterrupt < INTERRUPT_AGE_CAP) {
            ticksSinceInterrupt++;
        }
        if (!VISUALS.isEmpty()) {
            java.util.Iterator<Visual> visuals = VISUALS.values().iterator();
            while (visuals.hasNext()) {
                Visual visual = visuals.next();
                visual.age++;
                // Twice the announced duration is generous: a stop message normally ends the
                // visual; this is the safety net for one lost in a dimension change.
                if (visual.age > visual.drinkTicks * 2 + 40) {
                    visuals.remove();
                }
            }
        }
    }

    /** Leaving a server clears the mirror, so the next world never shows the last one's row. */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        last = null;
        ticksSinceMessage = 0;
        VISUALS.clear();
        pendingOutcome = DrinkVisualMessage.OUTCOME_NONE;
        ticksSinceInterrupt = INTERRUPT_AGE_CAP;
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
