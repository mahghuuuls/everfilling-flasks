package com.mahghuuuls.everfillingflasks.item;

import net.minecraft.item.EnumRarity;

import java.util.Locale;

/**
 * The three built-in Flasks (the epic tier was dropped by owner decision, 2026-08-25). The
 * tier fixes the registry name, the name colour (plain white since 1.1.0), and the default charge
 * count; every number a tier actually uses at runtime comes from the configuration.
 */
public enum FlaskTier {

    // All three are COMMON on purpose: the owner dropped the yellow and aqua name colours in 1.1.0
    // so the built-in Flasks and add-on Flasks read alike in a list. The constants keep their
    // names because they are the registry keys.
    COMMON(EnumRarity.COMMON, 2),
    UNCOMMON(EnumRarity.COMMON, 3),
    RARE(EnumRarity.COMMON, 4);

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
