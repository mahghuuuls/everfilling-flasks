package com.mahghuuuls.everfillingflasks.plant;

import com.mahghuuuls.everfillingflasks.item.InfusionKind;
import com.mahghuuuls.everfillingflasks.item.ModItems;
import net.minecraft.item.Item;

import java.util.Locale;

/**
 * The five wild plants: one for each built-in infusion and one for the Everlasting Seed. The
 * key is the block's registry path and the name of its config category. The item a plant gives
 * is looked up when it is needed, never when this enum loads, because the items exist only
 * after item registration.
 */
public enum WildPlant {

    SUNPETAL(PlantGround.GRASS, InfusionKind.SUNPETAL_LEAF, 0.5D),
    IRONROOT(PlantGround.GRASS_OR_STONE, InfusionKind.IRONROOT_SPRIG, 0.5D),
    QUICKMINT(PlantGround.GRASS_OR_DIRT, InfusionKind.QUICKMINT_LEAF, 0.5D),
    SECOND_WIND(PlantGround.GRASS, InfusionKind.SECOND_WIND_PETAL, 1.0D),
    EVERLASTING_SPROUT(PlantGround.GRASS_OR_DIRT, null, 1.0D);

    private final PlantGround ground;
    private final InfusionKind infusion;
    private final double defaultRegrowFactor;

    WildPlant(PlantGround ground, InfusionKind infusion, double defaultRegrowFactor) {
        this.ground = ground;
        this.infusion = infusion;
        this.defaultRegrowFactor = defaultRegrowFactor;
    }

    /** The regrow factor a plant has when the configuration does not set one. */
    public double defaultRegrowFactor() {
        return defaultRegrowFactor;
    }

    /** Registry path, translation key part, and config category name. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public PlantGround ground() {
        return ground;
    }

    /** The infusion this plant gives, or null for the plant that gives the Everlasting Seed. */
    public InfusionKind infusion() {
        return infusion;
    }

    /** The item a pick drops. Null before item registration. */
    public Item givenItem() {
        return infusion == null ? ModItems.seed() : ModItems.infusion(infusion);
    }
}
