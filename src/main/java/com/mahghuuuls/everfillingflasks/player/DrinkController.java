package com.mahghuuuls.everfillingflasks.player;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.diagnostics.Diagnostics;
import com.mahghuuuls.everfillingflasks.flask.EffectiveFlask;
import com.mahghuuuls.everfillingflasks.flask.FlaskMechanics;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import com.mahghuuuls.everfillingflasks.flask.ModifierRegistry;
import com.mahghuuuls.everfillingflasks.integration.InhibitedCompat;
import com.mahghuuuls.everfillingflasks.network.FlaskStateMessage;
import com.mahghuuuls.everfillingflasks.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * The server-side recharge engine, and the single writer of Flask charges and progress.
 *
 * <p>Owns the two cadence rules of the state contract: the equipped stack's NBT is written on a
 * charge change, on a slot change, on the clone at death, at logout, before a shift-click copies
 * the stack, and at most once per second while recharging (live progress sits in the capability
 * between flushes); the owner is sent a state message on any change and at most once per second
 * while progress is only ticking forward. The cadence decisions are the pure predicates
 * {@link #due(long, long)} so they can be tested against a fake clock.
 *
 * <p>Effective values are cached per equipped stack and recomputed on a slot change, a charge
 * rollover, and every state send, so modifier sources are not polled every tick.
 *
 * <p>Also the drink state machine: key edges arrive from the network handlers, the timer
 * runs in the tick, and completion spends, heals, and calls the hook with isolation.
 */
public final class DrinkController {

    /** Once per second, in ticks: the flush and steady-state sync cadence. */
    static final int CADENCE_TICKS = 20;

    /** Vanilla's drink-gurgle cadence while a use action runs. */
    private static final int DRINK_SOUND_TICKS = 7;

    /** One fixed identity so the slowdown can always be found and removed, never stacked. */
    private static final java.util.UUID SLOWDOWN_ID =
            java.util.UUID.fromString("5a2f6d5e-3b0a-4c56-9d5e-7f8f6f0e2a11");
    private static final String SLOWDOWN_NAME = "everfillingflasks drink slowdown";

    private static final java.util.Set<String> FAILED_HOOKS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private DrinkController() {
    }

    /** Called every server tick for every player, END phase. */
    public static void tick(EntityPlayerMP player) {
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data == null) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        trackSlotChange(data, now);

        // Modifier sources can change without any Flask event (a bauble unequipped, an effect
        // expiring), and at maximum charges no send would ever notice. The cache therefore
        // expires on the same one-second cadence as everything else.
        if (due(now, data.lastEffectiveRefreshTick)) {
            data.cachedEffective = null;
            data.lastEffectiveRefreshTick = now;
        }

        if (data.drinking) {
            drinkTick(player, data);
        }

        ItemStack flask = data.trackedStack;
        boolean advancing = false;
        if (!flask.isEmpty() && FlaskRegistry.isFlask(flask)) {
            advancing = recharge(player, data, flask, now);
        } else if (data.rechargePaused) {
            data.rechargePaused = false;
        }

        if (data.syncDirty || (advancing && due(now, data.lastSyncTick))) {
            sendState(player, data, now);
        }
    }

    /** The Use Flask key going down, already on the server thread. */
    public static void keyPressed(EntityPlayerMP player) {
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data == null || player.isDead) {
            return;
        }
        trackSlotChange(data, player.world.getTotalWorldTime());
        ItemStack flask = data.trackedStack;
        boolean valid = !flask.isEmpty() && FlaskRegistry.isFlask(flask);
        if (!valid) {
            Diagnostics.drinkRefused(player, "no flask equipped");
            return;
        }
        // Frozen for the whole drink: a modifier change mid-drink alters nothing committed.
        EffectiveFlask effective = computeEffective(player, flask);
        int charges = FlaskStackState.charges(flask);
        if (!FlaskMechanics.canStartDrink(true, charges, data.drinking)) {
            Diagnostics.drinkRefused(player, data.drinking ? "already drinking" : "no charges");
            return;
        }
        data.drinking = true;
        data.drinkElapsed = 0;
        data.drinkEffective = effective;
        data.drinkStack = flask;
        applySlowdown(player);
        sendDrinkVisual(player, true, effective.drinkTicks(), flask);
        playDrinkSound(player);
        Diagnostics.drinkStarted(player, effective.drinkTicks(), effective.healPercentage(),
                effective.hitThreshold());
        data.syncDirty = true;
    }

    /** The Use Flask key coming up, already on the server thread. The reason is display-only. */
    public static void keyReleased(EntityPlayerMP player, String reason) {
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data != null && data.drinking) {
            cancelDrink(player, data, reason);
        }
    }

    /**
     * A hit landing on a drinking player. Interrupts when the post-armor amount reaches the
     * threshold frozen at drink start; passive damage never reaches this method because the
     * guard only forwards damage with an attacker.
     */
    public static void hitTaken(EntityPlayerMP player, float amount, String attackerName) {
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data == null || !data.drinking) {
            return;
        }
        if (FlaskMechanics.interrupts(true, amount, data.drinkEffective.hitThreshold())) {
            cancelDrink(player, data, "hit interrupt " + amount + " from " + attackerName);
        }
    }

    /** Cancels an active drink, spending and healing nothing. Safe to call when idle. */
    public static void cancelDrink(EntityPlayerMP player, String reason) {
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data != null && data.drinking) {
            cancelDrink(player, data, reason);
        }
    }

    private static void cancelDrink(EntityPlayerMP player, FlaskPlayerData data, String reason) {
        clearDrinkState(player, data);
        Diagnostics.drinkCancelled(player, reason);
        data.syncDirty = true;
    }

    private static void clearDrinkState(EntityPlayerMP player, FlaskPlayerData data) {
        data.drinking = false;
        data.drinkElapsed = 0;
        data.drinkEffective = null;
        data.drinkStack = ItemStack.EMPTY;
        removeSlowdown(player);
        sendDrinkVisual(player, false, 0, ItemStack.EMPTY);
    }

    /** The third-person broadcast: watchers plus the drinker, on start and on any end. */
    private static void sendDrinkVisual(EntityPlayerMP player, boolean drinking, int drinkTicks,
                                        ItemStack flask) {
        com.mahghuuuls.everfillingflasks.network.DrinkVisualMessage message =
                new com.mahghuuuls.everfillingflasks.network.DrinkVisualMessage(
                        player.getEntityId(), drinking, drinkTicks, flask);
        PacketHandler.CHANNEL.sendToAllTracking(message, player);
        // The drinker's own copy: their first person reads the state mirror instead, so this
        // exists as the fallback identity for their third-person view if the state message lags.
        PacketHandler.CHANNEL.sendTo(message, player);
    }

    private static void drinkTick(EntityPlayerMP player, FlaskPlayerData data) {
        ItemStack flask = data.trackedStack;
        // The instance must still be the one the drink began with; a swap, removal, or any
        // replacement cancels even when the newcomer is also a Flask.
        if (flask != data.drinkStack || flask.isEmpty() || !FlaskRegistry.isFlask(flask)) {
            cancelDrink(player, data, "flask removed or invalid");
            return;
        }
        if (FlaskStackState.charges(flask) < 1) {
            cancelDrink(player, data, "charges reached zero");
            return;
        }
        data.drinkElapsed++;
        if (data.drinkElapsed % DRINK_SOUND_TICKS == 0) {
            playDrinkSound(player);
        }
        if (data.drinkElapsed >= data.drinkEffective.drinkTicks()) {
            complete(player, data, flask);
        }
        // No sync while merely progressing: the start message carried the duration and the
        // client interpolates. Only transitions mark dirty, keeping the no-per-tick contract.
    }

    /**
     * Spend, heal, go idle, then the hook: idle-before-hook on purpose, so a throwing hook can
     * never strand the machine mid-drink. Validation happened adjacent in drinkTick, in the
     * same tick, after trackSlotChange, which is the ordering that makes it sufficient.
     * Reaching full health mid-drink does not cancel; the heal then has nothing to add.
     */
    private static void complete(EntityPlayerMP player, FlaskPlayerData data, ItemStack flask) {
        EffectiveFlask effective = data.drinkEffective;
        int charges = FlaskStackState.charges(flask);
        FlaskStackState.setCharges(flask, charges - 1);
        float heal = FlaskMechanics.healAmount(player.getMaxHealth(),
                effective.healPercentage());
        if (heal > 0.0F) {
            player.heal(heal);
        }
        clearDrinkState(player, data);
        Diagnostics.drinkCompleted(player, charges - 1, effective.maxCharges(), heal);
        runCompletionHook(player, flask);
        playDrinkSound(player);
        data.syncDirty = true;
    }

    /** Hook isolation: a hook may do anything except break the Flask or the player. */
    private static void runCompletionHook(EntityPlayerMP player, ItemStack flask) {
        FlaskDefinition definition = FlaskRegistry.definition(flask);
        if (definition == null) {
            return;
        }
        try {
            definition.onDrinkCompleted(flask, player);
        } catch (Throwable failure) {
            if (FAILED_HOOKS.add(definition.getClass().getName())) {
                com.mahghuuuls.everfillingflasks.EverfillingFlasksMod.LOGGER.error(
                        "Flask completion hook {} failed; the drink itself completed normally",
                        definition.getClass().getName(), failure);
            }
        }
    }

    private static void applySlowdown(EntityPlayerMP player) {
        double slowdown = com.mahghuuuls.everfillingflasks.config.ConfigSnapshot.current()
                .drinkSlowdown() - 1.0D;
        if (slowdown == 0.0D) {
            return;
        }
        net.minecraft.entity.ai.attributes.IAttributeInstance speed = player.getEntityAttribute(
                net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed.getModifier(SLOWDOWN_ID) == null) {
            // Operation 2, multiply total; not saved to NBT, and removed on every exit path
            // plus defensively at login in case a crash mid-drink left it behind.
            net.minecraft.entity.ai.attributes.AttributeModifier modifier =
                    new net.minecraft.entity.ai.attributes.AttributeModifier(SLOWDOWN_ID,
                            SLOWDOWN_NAME, slowdown, 2);
            speed.applyModifier(modifier.setSaved(false));
        }
    }

    /** Public because login runs it defensively; a crash mid-drink must not outlive itself. */
    public static void removeSlowdown(EntityPlayerMP player) {
        net.minecraft.entity.ai.attributes.IAttributeInstance speed = player.getEntityAttribute(
                net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed.getModifier(SLOWDOWN_ID) != null) {
            speed.removeModifier(SLOWDOWN_ID);
        }
    }

    private static void playDrinkSound(EntityPlayerMP player) {
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.ENTITY_GENERIC_DRINK,
                net.minecraft.util.SoundCategory.PLAYERS, 0.5F,
                player.world.rand.nextFloat() * 0.1F + 0.9F);
    }

    /** Sends the owner a fresh state message regardless of cadence; used at login and respawn. */
    public static void syncNow(EntityPlayerMP player) {
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data != null) {
            long now = player.world.getTotalWorldTime();
            trackSlotChange(data, now);
            sendState(player, data, now);
        }
    }

    /** Writes live progress into the equipped stack; used at logout. The clone path and the
     * shift-click merge call {@link FlaskPlayerData#flushLiveProgress()} directly. */
    public static void flush(EntityPlayerMP player) {
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data != null) {
            data.flushLiveProgress();
        }
    }

    /**
     * A cadence clock is due when a full period has passed, and also when the recorded time is
     * ahead of the world clock, which happens when a player changes worlds or the recorded
     * value is uninitialized. Written this way so no sentinel can overflow the subtraction into
     * a comparison that never fires.
     */
    static boolean due(long now, long last) {
        if (last > now) {
            return true;
        }
        long elapsed = now - last;
        // A negative elapsed here can only mean the subtraction overflowed; due, not silent.
        return elapsed < 0 || elapsed >= CADENCE_TICKS;
    }

    /**
     * Detects the slot's content being replaced since the last tick, by instance. The departing
     * stack receives the unflushed live progress first, so taking a Flask out never costs the
     * ticks since the last one-second flush; the incoming stack's stored NBT becomes the new
     * live value. (A shift-click removal copies the stack instead of moving it; that path is
     * flushed eagerly by the container before the copy, and the emptied original left here is
     * skipped by the isEmpty check.)
     */
    private static void trackSlotChange(FlaskPlayerData data, long now) {
        ItemStack current = data.equippedFlask();
        if (current == data.trackedStack) {
            return;
        }
        data.flushLiveProgress();
        data.trackedStack = current;
        data.liveProgress = current.isEmpty() ? 0 : FlaskStackState.progress(current);
        data.liveValid = !current.isEmpty();
        data.cachedEffective = null;
        data.lastFlushTick = now;
        data.syncDirty = true;
    }

    /** One recharge tick. True while progress is actively moving forward. */
    private static boolean recharge(EntityPlayerMP player, FlaskPlayerData data, ItemStack flask,
                                    long now) {
        EffectiveFlask effective = effectiveFor(player, data, flask);
        int charges = FlaskStackState.charges(flask);
        int clamped = FlaskMechanics.clampCharges(charges, effective.maxCharges());
        if (clamped != charges) {
            FlaskStackState.setCharges(flask, clamped);
            charges = clamped;
            data.syncDirty = true;
        }

        if (charges >= effective.maxCharges()) {
            if (data.liveProgress != 0) {
                data.liveProgress = 0;
                FlaskStackState.setProgress(flask, 0);
                data.syncDirty = true;
            }
            if (data.rechargePaused) {
                data.rechargePaused = false;
                data.syncDirty = true;
            }
            return false;
        }

        boolean paused = InhibitedCompat.isInhibited(player);
        if (paused != data.rechargePaused) {
            data.rechargePaused = paused;
            data.syncDirty = true;
            if (paused) {
                Diagnostics.rechargePaused(player, data.liveProgress, effective.rechargeTicks());
            } else {
                Diagnostics.rechargeResumed(player, data.liveProgress, effective.rechargeTicks());
            }
        }
        if (paused) {
            return false;
        }

        FlaskMechanics.RechargeStep step =
                FlaskMechanics.advance(data.liveProgress, effective.rechargeTicks());
        data.liveProgress = step.progress();
        if (step.chargeGained()) {
            charges = FlaskMechanics.clampCharges(charges + 1, effective.maxCharges());
            FlaskStackState.setCharges(flask, charges);
            FlaskStackState.setProgress(flask, 0);
            data.lastFlushTick = now;
            data.cachedEffective = null;
            data.syncDirty = true;
            Diagnostics.chargeRestored(player, charges, effective.maxCharges());
        } else if (due(now, data.lastFlushTick)) {
            FlaskStackState.setProgress(flask, data.liveProgress);
            data.lastFlushTick = now;
        }
        return true;
    }

    private static EffectiveFlask effectiveFor(EntityPlayerMP player, FlaskPlayerData data,
                                               ItemStack flask) {
        if (data.cachedEffective == null) {
            data.cachedEffective = computeEffective(player, flask);
        }
        return data.cachedEffective;
    }

    private static EffectiveFlask computeEffective(EntityPlayerMP player, ItemStack flask) {
        FlaskDefinition definition = FlaskRegistry.definition(flask);
        return FlaskMechanics.effective(
                definition.maxCharges(flask, player),
                definition.healPercentage(flask, player),
                definition.rechargeTicks(flask, player),
                definition.drinkTicks(flask, player),
                definition.hitThreshold(flask, player),
                ModifierRegistry.collect(player));
    }

    private static void sendState(EntityPlayerMP player, FlaskPlayerData data, long now) {
        ItemStack flask = data.trackedStack;
        FlaskStateMessage message;
        if (flask.isEmpty() || !FlaskRegistry.isFlask(flask)) {
            message = FlaskStateMessage.empty();
        } else {
            // A send is the agreed moment to notice changed modifiers, so the cache refreshes
            // and the periodic expiry clock restarts, keeping it to one collect per second.
            data.cachedEffective = computeEffective(player, flask);
            data.lastEffectiveRefreshTick = now;
            EffectiveFlask effective = data.cachedEffective;
            int charges = FlaskMechanics.clampCharges(
                    FlaskStackState.charges(flask), effective.maxCharges());
            int drinkTicks = data.drinking ? data.drinkEffective.drinkTicks() : 1;
            message = new FlaskStateMessage(true, flask,
                    charges, effective.maxCharges(),
                    data.liveProgress, effective.rechargeTicks(), data.rechargePaused,
                    data.drinking, data.drinkElapsed, drinkTicks,
                    effective.hitThreshold());
            Diagnostics.stateSent(player, charges, effective.maxCharges(), data.liveProgress);
        }
        // The drinker's own copy: their first person reads the state mirror instead, so this
        // exists as the fallback identity for their third-person view if the state message lags.
        PacketHandler.CHANNEL.sendTo(message, player);
        data.lastSyncTick = now;
        data.syncDirty = false;
    }
}
