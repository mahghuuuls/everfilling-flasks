package com.mahghuuuls.everfillingflasks.item;

import java.util.Locale;

/**
 * The four built-in infusions, herb-themed by the owner's 2026-08-25 naming decision
 * ("things we add in a drink"). The kind fixes the registry name and what its strength means.
 * Every number a kind uses, cost and strength alike, comes from the configuration file, which
 * is the only source of those numbers (the unused defaults that once sat here were removed in
 * 1.2.0 because they disagreed with the config and misled readers).
 */
public enum InfusionKind {

    /** +healing per piece; strength is the fraction. */
    SUNPETAL_LEAF(Effect.HEALING),
    /** +hit threshold per piece; strength is the fraction. */
    IRONROOT_SPRIG(Effect.HIT_THRESHOLD),
    /** +drink speed per piece; strength is the fraction. */
    QUICKMINT_LEAF(Effect.DRINK_SPEED),
    /** Regeneration after a completed drink; strength is the duration in seconds. */
    SECOND_WIND_PETAL(Effect.POST_DRINK_REGEN);

    /** What a kind's strength number feeds. */
    public enum Effect {
        HEALING, HIT_THRESHOLD, DRINK_SPEED, POST_DRINK_REGEN
    }

    private final Effect effect;

    InfusionKind(Effect effect) {
        this.effect = effect;
    }

    public Effect effect() {
        return effect;
    }

    /** Lowercase name used in config keys, registry names, and recipe switches. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
