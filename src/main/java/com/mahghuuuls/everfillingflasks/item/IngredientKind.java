package com.mahghuuuls.everfillingflasks.item;

import java.util.Locale;

/**
 * The four built-in ingredients, herb-themed by the owner's 2026-08-25 naming decision
 * ("things we add in a drink"). The kind fixes the registry name and what its strength means;
 * every number a kind actually uses at runtime comes from the configuration.
 */
public enum IngredientKind {

    /** +healing per piece. Strength is the fraction: 0.10 is +10 percent. */
    SUNPETAL_LEAF(2, 0.10, Effect.HEALING),
    /** +hit threshold per piece. Strength 0.40 is +40 percent (owner buff, 2026-08-25). */
    IRONROOT_SPRIG(2, 0.40, Effect.HIT_THRESHOLD),
    /** +drink speed per piece. Strength 0.20 is +20 percent. */
    QUICKMINT_LEAF(2, 0.20, Effect.DRINK_SPEED),
    /** Regeneration after a completed drink. Strength is the duration in seconds. */
    SECOND_WIND_PETAL(3, 5.0, Effect.POST_DRINK_REGEN);

    /** What a kind's strength number feeds. */
    public enum Effect {
        HEALING, HIT_THRESHOLD, DRINK_SPEED, POST_DRINK_REGEN
    }

    private final int defaultCost;
    private final double defaultStrength;
    private final Effect effect;

    IngredientKind(int defaultCost, double defaultStrength, Effect effect) {
        this.defaultCost = defaultCost;
        this.defaultStrength = defaultStrength;
        this.effect = effect;
    }

    public int defaultCost() {
        return defaultCost;
    }

    public double defaultStrength() {
        return defaultStrength;
    }

    public Effect effect() {
        return effect;
    }

    /** Lowercase name used in config keys, registry names, and recipe switches. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
