package com.mahghuuuls.everfillingflasks.flask;

/**
 * The result of applying one player's Flask modifiers to one Flask's base values. Immutable;
 * built by {@link FlaskMechanics#effective}. A drink freezes one of these at its start, so a
 * modifier change mid-drink cannot stretch or shrink the drink already in progress.
 */
public final class EffectiveFlask {

    private final int maxCharges;
    private final float healPercentage;
    private final int rechargeTicks;
    private final int drinkTicks;
    private final float hitThreshold;
    private final float effectPower;

    EffectiveFlask(int maxCharges, float healPercentage, int rechargeTicks, int drinkTicks,
                   float hitThreshold, float effectPower) {
        this.maxCharges = maxCharges;
        this.healPercentage = healPercentage;
        this.rechargeTicks = rechargeTicks;
        this.drinkTicks = drinkTicks;
        this.hitThreshold = hitThreshold;
        this.effectPower = effectPower;
    }

    public int maxCharges() {
        return maxCharges;
    }

    public float healPercentage() {
        return healPercentage;
    }

    public int rechargeTicks() {
        return rechargeTicks;
    }

    public int drinkTicks() {
        return drinkTicks;
    }

    public float hitThreshold() {
        return hitThreshold;
    }

    /** {@code 1 + effect power sum}, never below 0; carried to the Flask, never used here. */
    public float effectPower() {
        return effectPower;
    }
}
