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
        int maxCharges = Math.max(MIN_MAX_CHARGES, baseMaxCharges + bonuses.maxChargesFlat());
        float heal = baseHealPercentage * multiplier(bonuses.healingSum());
        int recharge = Math.max(MIN_RECHARGE_TICKS,
                divideByMultiplier(Math.max(MIN_BASE_TICKS, baseRechargeTicks),
                        bonuses.rechargeSpeedSum()));
        int drink = Math.max(MIN_DRINK_TICKS,
                divideByMultiplier(Math.max(MIN_BASE_TICKS, baseDrinkTicks),
                        bonuses.drinkSpeedSum()));
        float threshold = baseHitThreshold * multiplier(bonuses.hitResistanceSum());
        return new EffectiveFlask(maxCharges, heal, recharge, drink, threshold);
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
     * The drink-start rule: a valid Flask with a charge, not already drinking, and not over
     * capacity. Health does not matter: a full-health drink is allowed and simply wastes its
     * heal, because an add-on Flask can carry a completion effect a player wants at any health.
     */
    public static boolean canStartDrink(boolean validFlask, int charges, boolean alreadyDrinking,
                                        boolean overCapacity) {
        return validFlask && charges >= 1 && !alreadyDrinking && !overCapacity;
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
