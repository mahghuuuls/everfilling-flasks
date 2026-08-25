package com.mahghuuuls.everfillingflasks.api;

import net.minecraft.item.ItemStack;

/**
 * A read-only view of one player's Flask state, for HUDs and other presentation. Constructed
 * by the core; add-ons only read it. It carries effective values, with every modifier already
 * applied, and it cannot change gameplay: there is deliberately no setter and no reference to
 * the live capability.
 *
 * <p>Progress values are in ticks so a renderer can interpolate between updates.
 */
public final class FlaskSnapshot {

    private final ItemStack flask;
    private final int charges;
    private final int maxCharges;
    private final int rechargeProgressTicks;
    private final int rechargeTicks;
    private final boolean rechargePaused;
    private final boolean drinking;
    private final int drinkProgressTicks;
    private final int drinkTicks;
    private final float hitThreshold;

    /** Constructed by the mod. Add-ons receive instances; they do not build them. */
    public FlaskSnapshot(ItemStack flask, int charges, int maxCharges, int rechargeProgressTicks,
                         int rechargeTicks, boolean rechargePaused, boolean drinking,
                         int drinkProgressTicks, int drinkTicks, float hitThreshold) {
        // Null tolerated so a construction mistake cannot become an NPE inside a HUD renderer.
        this.flask = flask == null ? ItemStack.EMPTY : flask;
        this.charges = charges;
        this.maxCharges = maxCharges;
        this.rechargeProgressTicks = rechargeProgressTicks;
        this.rechargeTicks = rechargeTicks;
        this.rechargePaused = rechargePaused;
        this.drinking = drinking;
        this.drinkProgressTicks = drinkProgressTicks;
        this.drinkTicks = drinkTicks;
        this.hitThreshold = hitThreshold;
    }

    public boolean hasFlask() {
        return !flask.isEmpty();
    }

    /** The equipped Flask, as a copy or the synced client stack. Mutating it changes nothing. */
    public ItemStack flask() {
        return flask;
    }

    public int charges() {
        return charges;
    }

    /** Effective maximum, flat modifiers included. */
    public int maxCharges() {
        return maxCharges;
    }

    public int rechargeProgressTicks() {
        return rechargeProgressTicks;
    }

    /** Effective ticks per charge. 1 in the empty snapshot, so ratio math never divides by 0. */
    public int rechargeTicks() {
        return rechargeTicks;
    }

    /** True while Inhibited freezes recharge. */
    public boolean rechargePaused() {
        return rechargePaused;
    }

    public boolean drinking() {
        return drinking;
    }

    public int drinkProgressTicks() {
        return drinkProgressTicks;
    }

    /** Effective drink duration for the current or next drink. */
    public int drinkTicks() {
        return drinkTicks;
    }

    /** Effective hit threshold in half-hearts. */
    public float hitThreshold() {
        return hitThreshold;
    }
}
