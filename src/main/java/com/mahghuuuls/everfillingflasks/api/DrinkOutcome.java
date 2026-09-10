package com.mahghuuuls.everfillingflasks.api;

/**
 * What one completed drink actually did, handed to
 * {@link FlaskDefinition#onDrinkCompleted(net.minecraft.item.ItemStack,
 * net.minecraft.entity.player.EntityPlayer, DrinkOutcome)} on the logical server. Immutable.
 *
 * <p>Health values are in health points (half-hearts), the unit {@code EntityPlayer#getHealth}
 * uses. {@link #healApplied()} is measured, not computed: the core reads health before and after
 * its one heal call, so a player at full health reads 0 and another mod's heal listener is
 * reflected honestly. Since 1.1.0. These accessors are frozen for 1.x.
 */
public final class DrinkOutcome {

    private final float healthBefore;
    private final float healRequested;
    private final float healApplied;
    private final float healOverTimeScheduled;
    private final int chargesLeft;
    private final float effectPower;

    /** Built by the core only; add-ons receive one, they never make one. */
    public DrinkOutcome(float healthBefore, float healRequested, float healApplied,
                        float healOverTimeScheduled, int chargesLeft, float effectPower) {
        this.healthBefore = healthBefore;
        this.healRequested = healRequested;
        this.healApplied = healApplied;
        this.healOverTimeScheduled = healOverTimeScheduled;
        this.chargesLeft = chargesLeft;
        this.effectPower = effectPower;
    }

    /** The player's health just before the instant heal. */
    public float healthBefore() {
        return healthBefore;
    }

    /** The instant heal the core computed from the effective heal percentage. */
    public float healRequested() {
        return healRequested;
    }

    /** Health actually gained by the instant heal: after minus before, never below 0. */
    public float healApplied() {
        return healApplied;
    }

    /**
     * The over-time heal the core has scheduled to pay out after this drink, in health points;
     * 0 when the Flask declares none.
     */
    public float healOverTimeScheduled() {
        return healOverTimeScheduled;
    }

    /** Charges left on the Flask after this drink's charge was spent. */
    public int chargesLeft() {
        return chargesLeft;
    }

    /**
     * The effect power multiplier: 1 plus every source's {@link FlaskBonuses#effectPower}
     * contribution, never below 0. The core does nothing with it; a Flask scales whatever its
     * secondary effect is by this, if it has one.
     */
    public float effectPower() {
        return effectPower;
    }
}
