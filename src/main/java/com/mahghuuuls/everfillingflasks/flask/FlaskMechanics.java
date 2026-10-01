package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;

/**
 * Every Flask formula, in one place, with no Minecraft imports.
 *
 * <p>This class is the single owner of how bonuses combine, how the drink and recharge floors
 * apply, what counts as a hit interrupt, and how much a drink heals. Nothing else in the mod
 * may restate one of these rules; callers pass bases in and use the result.
 */
public final class FlaskMechanics {

    /** Modifiers can never shorten a drink below this, no matter how large the bonus. */
    public static final int MIN_DRINK_TICKS = 5;

    /** Recharge can never drop below one tick per charge. */
    public static final int MIN_RECHARGE_TICKS = 1;

    /** A Flask always has at least one charge slot, whatever the flat modifiers say. */
    public static final int MIN_MAX_CHARGES = 1;

    /** A Flask has at least one infusion slot, and at most the two rows the screen draws. */
    public static final int MIN_INFUSION_SLOTS = 1;
    public static final int MAX_INFUSION_SLOTS = 12;

    /** Base durations below one tick are treated as one tick (the FlaskDefinition contract). */
    private static final int MIN_BASE_TICKS = 1;

    /** An over-time heal runs at most an hour; longer is a mistake, not a design. */
    public static final int MAX_PAYOUT_TICKS = 72000;

    private FlaskMechanics() {
    }

    /**
     * Applies {@code bonuses} to the base values. Percentage sums multiply the base and are
     * clamped so the multiplier never drops below 0; speed bonuses divide durations; the flat
     * charge bonus adds. Floors: {@link #MIN_DRINK_TICKS}, {@link #MIN_RECHARGE_TICKS},
     * {@link #MIN_MAX_CHARGES}.
     */
    public static EffectiveFlask effective(int baseMaxCharges, float baseHealPercentage,
                                           int baseRechargeTicks, int baseDrinkTicks,
                                           float baseHitThreshold, FlaskBonuses bonuses) {
        return effective(baseMaxCharges, baseHealPercentage, baseRechargeTicks, baseDrinkTicks,
                baseHitThreshold, 0.0F, 0, bonuses);
    }

    /**
     * As {@link #effective(int, float, int, int, float, FlaskBonuses)}, with an over-time heal
     * (REQ-047). The over-time fraction takes the same healing multiplier as the instant part;
     * the fraction is clamped to 0..1 and the ticks to 0..{@link #MAX_PAYOUT_TICKS}.
     */
    public static EffectiveFlask effective(int baseMaxCharges, float baseHealPercentage,
                                           int baseRechargeTicks, int baseDrinkTicks,
                                           float baseHitThreshold, float baseOverTimePercentage,
                                           int baseOverTimeTicks, FlaskBonuses bonuses) {
        int maxCharges = Math.max(MIN_MAX_CHARGES, baseMaxCharges + bonuses.maxChargesFlat());
        float healMultiplier = multiplier(bonuses.healingSum());
        float heal = baseHealPercentage * healMultiplier;
        int recharge = Math.max(MIN_RECHARGE_TICKS,
                divideByMultiplier(Math.max(MIN_BASE_TICKS, baseRechargeTicks),
                        bonuses.rechargeSpeedSum()));
        int drink = Math.max(MIN_DRINK_TICKS,
                divideByMultiplier(Math.max(MIN_BASE_TICKS, baseDrinkTicks),
                        bonuses.drinkSpeedSum()));
        float threshold = baseHitThreshold * multiplier(bonuses.hitResistanceSum());
        float effectPower = multiplier(bonuses.effectPowerSum());
        float overTime = Math.max(0.0F, Math.min(1.0F, baseOverTimePercentage)) * healMultiplier;
        int overTimeTicks = Math.max(0, Math.min(MAX_PAYOUT_TICKS, baseOverTimeTicks));
        return new EffectiveFlask(maxCharges, heal, recharge, drink, threshold, effectPower,
                overTime, overTimeTicks);
    }

    /**
     * One tick of an over-time payout (REQ-047): an even share of what is still owed over the
     * ticks still to run. Recomputed from the remainder each tick, so the shares always sum to
     * the total exactly: on the last tick the division is by one, which pays whatever is left
     * to the bit. Zero when nothing is owed or no ticks remain.
     */
    public static float payoutStep(float remainingPoints, int remainingTicks) {
        if (remainingTicks <= 0 || remainingPoints <= 0.0F) {
            return 0.0F;
        }
        return remainingPoints / remainingTicks;
    }

    /**
     * Health actually gained by one heal call: after minus before, never below 0. Below 0 can
     * happen only if something else lowered health between the two reads, which the core does
     * not do; the floor keeps the reported value honest rather than negative.
     */
    public static float healApplied(float healthBefore, float healthAfter) {
        return Math.max(0.0F, healthAfter - healthBefore);
    }

    /** A declared slot count brought inside the range the screen can draw. */
    public static int infusionSlots(int declared) {
        return Math.max(MIN_INFUSION_SLOTS, Math.min(MAX_INFUSION_SLOTS, declared));
    }

    /** {@code 1 + sum}, never below 0. */
    static float multiplier(float sum) {
        return Math.max(0.0F, 1.0F + sum);
    }

    /**
     * A duration divided by a speed multiplier. A multiplier clamped to 0 would divide by zero,
     * which here means "as slow as possible": the duration saturates instead.
     */
    private static int divideByMultiplier(int duration, float speedSum) {
        float m = multiplier(speedSum);
        if (m <= 0.0F) {
            return Integer.MAX_VALUE;
        }
        return Math.round(duration / m);
    }

    /**
     * One recharge tick. The result carries both halves of the rule so no caller re-states it:
     * on rollover the progress in the result is already 0 and one charge was gained. Exact
     * boundary: progress {@code rechargeTicks - 1} plus one tick rolls over.
     */
    public static RechargeStep advance(int progress, int rechargeTicks) {
        if (progress + 1 >= rechargeTicks) {
            return new RechargeStep(0, true);
        }
        return new RechargeStep(progress + 1, false);
    }

    /**
     * Many recharge ticks at once, for an add-on pushing the refill forward (REQ-048). The same
     * rule as {@link #advance} applied {@code ticks} times, in closed form: a charge completes
     * after {@code max(1, rechargeTicks - progress)} more ticks, the remainder carries into the
     * next charge, and the Flask never exceeds {@code maxCharges}. Ticks past a full Flask are
     * not applied, so {@link AdvanceResult#ticksApplied} is at most {@code ticks} and at most
     * what full needed. Zero or fewer ticks, or an already full Flask, apply nothing.
     */
    public static AdvanceResult advanceBy(int progress, int ticks, int rechargeTicks,
                                          int charges, int maxCharges) {
        int applied = 0;
        while (ticks > 0 && charges < maxCharges) {
            int needed = Math.max(1, rechargeTicks - progress);
            if (ticks >= needed) {
                ticks -= needed;
                applied += needed;
                charges++;
                progress = 0;
            } else {
                progress += ticks;
                applied += ticks;
                ticks = 0;
            }
        }
        return new AdvanceResult(progress, charges, applied);
    }

    /** The outcome of {@link #advanceBy}: where progress and charges ended, and the ticks used. */
    public static final class AdvanceResult {

        private final int progress;
        private final int charges;
        private final int ticksApplied;

        AdvanceResult(int progress, int charges, int ticksApplied) {
            this.progress = progress;
            this.charges = charges;
            this.ticksApplied = ticksApplied;
        }

        public int progress() {
            return progress;
        }

        public int charges() {
            return charges;
        }

        public int ticksApplied() {
            return ticksApplied;
        }
    }

    /** The outcome of one recharge tick: the stored progress and whether a charge was gained. */
    public static final class RechargeStep {

        private final int progress;
        private final boolean chargeGained;

        RechargeStep(int progress, boolean chargeGained) {
            this.progress = progress;
            this.chargeGained = chargeGained;
        }

        public int progress() {
            return progress;
        }

        public boolean chargeGained() {
            return chargeGained;
        }
    }

    /** Current charges never exceed the effective maximum; shrinking modifiers cut them. */
    public static int clampCharges(int charges, int maxCharges) {
        return Math.max(0, Math.min(charges, maxCharges));
    }

    /**
     * The hit-interrupt rule: only damage with an attacker interrupts, and only at or above
     * the effective threshold. Amounts are post-armor half-hearts.
     */
    public static boolean interrupts(boolean hasAttacker, float amount, float threshold) {
        return hasAttacker && amount >= threshold;
    }

    /** Half-hearts healed by a completed drink. The game clamps to maximum health afterwards. */
    public static float healAmount(float maxHealth, float effectiveHealPercentage) {
        return maxHealth * effectiveHealPercentage;
    }

    /**
     * The drink-start rule: a valid Flask with a charge, not already drinking, and an infusion
     * grid that is not inert (over capacity, or since 1.2.0 holding a conflicting pair). Health
     * does not matter: a full-health drink is allowed and simply wastes its heal, because an
     * add-on Flask can carry a completion effect a player wants at any health.
     */
    public static boolean canStartDrink(boolean validFlask, int charges, boolean alreadyDrinking,
                                        boolean inertGrid) {
        return validFlask && charges >= 1 && !alreadyDrinking && !inertGrid;
    }

    /**
     * The over-capacity rule, the infusion system's one genuinely new formula: placed costs
     * strictly above the potency make the Flask unusable. Exactly full is fine. Recharge is
     * untouched; only drinking is blocked, and removing pieces restores use at once.
     */
    public static boolean overCapacity(int usedPotency, int potency) {
        return usedPotency > Math.max(0, potency);
    }

    /**
     * The client's guess at recharge progress between server updates: the last known value plus
     * elapsed ticks, frozen while paused or at maximum charges, and capped just short of the
     * threshold so a display never claims a charge the server has not granted.
     */
    public static int interpolateProgress(int knownProgress, int ticksSinceKnown, boolean paused,
                                          boolean atMaxCharges, int rechargeTicks) {
        if (paused || atMaxCharges) {
            return Math.min(knownProgress, Math.max(0, rechargeTicks - 1));
        }
        return Math.min(knownProgress + Math.max(0, ticksSinceKnown),
                Math.max(0, rechargeTicks - 1));
    }
}
