package com.mahghuuuls.everfillingflasks.api;

/**
 * Accumulates the bonuses of every modifier source for one computation.
 *
 * <p>Percentage bonuses of one kind add together and then multiply the base value: two sources
 * adding {@code 0.5f} and {@code 0.3f} healing give {@code base * 1.8}. Negative bonuses are
 * allowed; the core clamps each combined multiplier so it never drops below 0. Maximum charges
 * take flat additions instead, because a percentage of small whole numbers rounds badly.
 */
public final class FlaskBonuses {

    private float healingSum;
    private float drinkSpeedSum;
    private float hitResistanceSum;
    private float rechargeSpeedSum;
    private int maxChargesFlat;

    /** Add a healing bonus: {@code 0.5f} means 50 percent more healing. */
    public void healing(float percent) {
        healingSum += percent;
    }

    /** Add a drink-speed bonus: {@code 1.0f} means drinking twice as fast. */
    public void drinkSpeed(float percent) {
        drinkSpeedSum += percent;
    }

    /** Add a hit-resistance bonus: {@code 1.0f} doubles the hit threshold. */
    public void hitResistance(float percent) {
        hitResistanceSum += percent;
    }

    /** Add a recharge-speed bonus: {@code 1.0f} means recharging twice as fast. */
    public void rechargeSpeed(float percent) {
        rechargeSpeedSum += percent;
    }

    /** Add whole extra charges. Negative values are allowed; the core keeps at least 1. */
    public void maxCharges(int flat) {
        maxChargesFlat += flat;
    }

    public float healingSum() {
        return healingSum;
    }

    public float drinkSpeedSum() {
        return drinkSpeedSum;
    }

    public float hitResistanceSum() {
        return hitResistanceSum;
    }

    public float rechargeSpeedSum() {
        return rechargeSpeedSum;
    }

    public int maxChargesFlat() {
        return maxChargesFlat;
    }
}
