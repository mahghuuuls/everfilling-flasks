package com.mahghuuuls.everfillingflasks.plant;

import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/**
 * The two stages of a wild plant. A bloom can be picked; a stem is what stays after picking and
 * blooms again once its regrow time has passed. The ordinal is the block metadata, a saved-world
 * contract: bloom is 0, stem is 1.
 */
public enum PlantStage implements IStringSerializable {

    BLOOM,
    STEM;

    @Override
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The stage for a metadata value; anything unknown reads as a bloom. */
    public static PlantStage fromMeta(int meta) {
        return meta == STEM.ordinal() ? STEM : BLOOM;
    }
}
