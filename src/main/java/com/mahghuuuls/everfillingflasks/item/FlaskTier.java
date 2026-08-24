package com.mahghuuuls.everfillingflasks.item;

import net.minecraft.item.EnumRarity;

import java.util.Locale;

/**
 * The four built-in Flasks. The tier fixes the registry name, the rarity color, and the default
 * charge count; every number a tier actually uses at runtime comes from the configuration.
 */
public enum FlaskTier {

    COMMON(EnumRarity.COMMON, 1),
    UNCOMMON(EnumRarity.UNCOMMON, 2),
    RARE(EnumRarity.RARE, 3),
    EPIC(EnumRarity.EPIC, 4);

    private final EnumRarity rarity;
    private final int defaultMaxCharges;

    FlaskTier(EnumRarity rarity, int defaultMaxCharges) {
        this.rarity = rarity;
        this.defaultMaxCharges = defaultMaxCharges;
    }

    public EnumRarity rarity() {
        return rarity;
    }

    public int defaultMaxCharges() {
        return defaultMaxCharges;
    }

    /** Lowercase name used in config keys and registry names: {@code common_flask}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String registryName() {
        return key() + "_flask";
    }
}
