package com.mahghuuuls.everfillingflasks.plant;

import com.mahghuuuls.everfillingflasks.item.InfusionKind;
import com.mahghuuuls.everfillingflasks.item.ModItems;
import net.minecraft.item.Item;

import java.util.Locale;

/**
 * The five wild plants: one for each built-in infusion and one for the Everlasting Seed. The
 * key is the block's registry path and the name of its config category, where its chance,
 * biomes, and regrow factor are set. The item a plant gives
 * is looked up when it is needed, never when this enum loads, because the items exist only
 * after item registration.
 */
public enum WildPlant {

    SUNPETAL(PlantGround.GRASS, InfusionKind.SUNPETAL_LEAF, 0, false, 0.85D),
    IRONROOT(PlantGround.GRASS_OR_STONE, InfusionKind.IRONROOT_SPRIG, 0, false, 0.85D),
    QUICKMINT(PlantGround.GRASS_OR_DIRT, InfusionKind.QUICKMINT_LEAF, 0, false, 0.85D),
    SECOND_WIND(PlantGround.GRASS, InfusionKind.SECOND_WIND_PETAL, 0, false, 0.85D),
    /** The mystic one: a faint glow, noticeable at night and less than a torch, and a rare sparkle. */
    EVERLASTING_SPROUT(PlantGround.GRASS_OR_DIRT, null, 5, true, 0.85D);

    private final PlantGround ground;
    private final InfusionKind infusion;
    private final int bloomLight;
    private final boolean sparkles;
    private final double bloomTop;

    WildPlant(PlantGround ground, InfusionKind infusion, int bloomLight, boolean sparkles, double bloomTop) {
        this.ground = ground;
        this.infusion = infusion;
        this.bloomLight = bloomLight;
        this.sparkles = sparkles;
        this.bloomTop = bloomTop;
    }

    /** The light a plant gives off in bloom; a stem gives none. */
    public int bloomLight() {
        return bloomLight;
    }

    /** Whether a plant in bloom shows a sparkle now and then. */
    public boolean sparkles() {
        return sparkles;
    }

    /** The top of the bloom's click and outline box, in blocks: at or above its model's highest point. */
    public double bloomTop() {
        return bloomTop;
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

    /** The plant that gives this item, or null when no wild plant gives it. */
    public static WildPlant givingItem(Item item) {
        if (item == null) {
            return null;
        }
        for (WildPlant plant : values()) {
            if (plant.givenItem() == item) {
                return plant;
            }
        }
        return null;
    }
}
