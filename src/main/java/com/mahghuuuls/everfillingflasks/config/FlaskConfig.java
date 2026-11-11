package com.mahghuuuls.everfillingflasks.config;

import com.mahghuuuls.everfillingflasks.Tags;
import net.minecraftforge.common.config.Config;

/**
 * Every option this mod has. Nothing reads these fields directly except
 * {@link ConfigSnapshot#refresh()}; the rest of the mod reads the snapshot, so validation and
 * clamping happen once. Every value takes effect on the next game start.
 *
 * <p>Option fields must be public: Forge's ConfigManager skips every non-public field, so a
 * private option would silently keep its default. Keep anything that is not an option out of
 * this class entirely.
 */
@Config(modid = Tags.MOD_ID, name = Tags.MOD_ID)
public final class FlaskConfig {

    @Config.Name("general")
    @Config.Comment("Options that are not tied to one Flask tier.")
    public static final General general = new General();

    @Config.Name("flasks")
    @Config.Comment("The three built-in Flask tiers. Times are in ticks; 20 ticks are 1 second.")
    public static final Flasks flasks = new Flasks();

    @Config.Name("infusions")
    @Config.Comment({
            "The four built-in Flask Infusions. cost is potency used per placed piece.",
            "strength is the effect size: a fraction for the percentage infusions",
            "(0.08 is +8 percent per piece), seconds for the Second Wind Petal."})
    public static final Infusions infusions = new Infusions();

    @Config.Name("recipes")
    @Config.Comment("One switch per built-in recipe. false removes that recipe entirely.")
    public static final Recipes recipes = new Recipes();

    @Config.Name("journal")
    @Config.Comment("The in-game journal. These options change words only, never behaviour.")
    public static final Journal journal = new Journal();

    @Config.Name("plants")
    @Config.Comment({
            "The five wild plants. Each grows alone in new Overworld chunks; hitting one in bloom",
            "gives its item and leaves a stem that blooms again later. Land generated before",
            "never gets plants. Every value takes effect on the next game start."})
    public static final Plants plants = new Plants();

    public static final class General {

        @Config.Name("startingFlask")
        @Config.Comment({
                "Registry name of the Flask a new player receives once, for example",
                "everfillingflasks:common_flask. Empty grants nothing.",
                "Takes effect on next game start."})
        public String startingFlask = Tags.MOD_ID + ":common_flask";

        @Config.Name("keepFlaskOnDeath")
        @Config.Comment({
                "Keep the equipped Flask in its slot through death.",
                "When false it drops like the rest of the inventory."})
        public boolean keepFlaskOnDeath = true;

        @Config.Name("drinkSlowdown")
        @Config.Comment({
                "Walking speed while drinking, as a fraction of normal speed.",
                "0.5 is half speed; 1.0 disables the slowdown."})
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double drinkSlowdown = 0.5;

        @Config.Name("infusionLoot")
        @Config.Comment({
                "Also put Everlasting Seeds and Flask Infusions in dungeon, mineshaft, and village",
                "blacksmith chests. Off by default: they grow as wild plants (see plants). A file",
                "written by 1.3.0 or earlier keeps its own value; only a missing key takes the",
                "default. With chest loot off and a plant turned off, its item has no source in",
                "survival."})
        public boolean infusionLoot = false;

        @Config.Name("inhibitedIntegration")
        @Config.Comment({
                "Off by default. true makes a player's Flask stop recharging while they have the",
                "Inhibited effect, for packs that use a mod providing it. Does nothing without one.",
                "A file written by 1.0.0 keeps its own value; only a missing key takes the default."})
        public boolean inhibitedIntegration = false;

        @Config.Name("plantRegrowMinutes")
        @Config.Comment({
                "Minutes of world time a picked wild plant needs to bloom again, before its",
                "plant's regrowFactor. 180 is three hours of play; 0.5 halves it for a plant."})
        @Config.RangeInt(min = 1, max = 100000)
        public int plantRegrowMinutes = 180;

        @Config.Name("diagnostics")
        @Config.Comment({
                "Log one line per Flask decision: drink started, refused (and why), cancelled",
                "(and why), completed, charge restored, recharge paused; and one line per wild",
                "plant placed in a new chunk, with its position and biome, to find plants without",
                "searching. For pack authors and server owners investigating reports. Off for",
                "normal play."})
        public boolean diagnostics = false;
    }

    public static final class Flasks {

        @Config.Name("common")
        public final TierValues common = new TierValues(2);

        @Config.Name("uncommon")
        public final TierValues uncommon = new TierValues(3);

        @Config.Name("rare")
        public final TierValues rare = new TierValues(4);
    }

    public static final class TierValues {

        @Config.Name("maxCharges")
        @Config.Comment("Charges this Flask holds when full.")
        @Config.RangeInt(min = 1, max = 64)
        public int maxCharges;

        @Config.Name("healPercentage")
        @Config.Comment("Health restored per drink, as a fraction of maximum health. 0.33 is 33 percent.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double healPercentage = 0.33;

        @Config.Name("rechargeTicks")
        @Config.Comment("Ticks to restore one missing charge. 600 is 30 seconds.")
        @Config.RangeInt(min = 1, max = 72000)
        public int rechargeTicks = 600;

        @Config.Name("drinkTicks")
        @Config.Comment("Ticks the key must be held for one drink. 30 is 1.5 seconds.")
        @Config.RangeInt(min = 1, max = 1200)
        public int drinkTicks = 30;

        @Config.Name("hitThreshold")
        @Config.Comment({
                "Damage from an attacker, in half-hearts after armor, that cancels a drink in",
                "progress. 1.0 means any real hit; fire, poison, and falls never cancel."})
        @Config.RangeDouble(min = 0.0, max = 1000.0)
        public double hitThreshold = 1.0;

        @Config.Name("potency")
        @Config.Comment({
                "Infusion budget of this Flask's infusion grid. Placed infusions whose",
                "summed costs exceed it make the Flask unusable until pieces are removed.",
                "Every tier shares the same default on purpose: tiers differ by charges."})
        @Config.RangeInt(min = 0, max = 1000)
        public int potency = 10;

        TierValues(int maxCharges) {
            this.maxCharges = maxCharges;
        }
    }

    public static final class Infusions {

        @Config.Name("sunpetalLeaf")
        @Config.Comment("+healing per piece; strength 0.08 is +8 percent.")
        public final InfusionValues sunpetalLeaf = new InfusionValues(2, 0.08);

        @Config.Name("ironrootSprig")
        @Config.Comment("+damage threshold per piece; strength 0.32 is +32 percent.")
        public final InfusionValues ironrootSprig = new InfusionValues(2, 0.32);

        @Config.Name("quickmintLeaf")
        @Config.Comment("+drink speed per piece; strength 0.16 is +16 percent.")
        public final InfusionValues quickmintLeaf = new InfusionValues(2, 0.16);

        @Config.Name("secondWindPetal")
        @Config.Comment("Regeneration after a completed drink; strength is seconds.")
        public final InfusionValues secondWindPetal = new InfusionValues(3, 5.0);
    }

    public static final class InfusionValues {

        @Config.Name("cost")
        @Config.Comment("Potency used per placed piece.")
        @Config.RangeInt(min = 0, max = 1000)
        public int cost;

        @Config.Name("strength")
        @Config.Comment("Effect size; see the infusion's own comment for its unit.")
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double strength;

        InfusionValues(int cost, double strength) {
            this.cost = cost;
            this.strength = strength;
        }
    }

    public static final class Journal {

        @Config.Name("textOverrides")
        @Config.Comment({
                "Replace or hide the text on a journal entry, one entry per line.",
                "everfillingflasks:sunpetal_leaf=Traded by wandering herbalists. replaces it;",
                "everfillingflasks:sunpetal_leaf= (nothing after the equals sign) hides it.",
                "Use the item's registry name, never its display name, so the setting survives",
                "a language change. A name nothing matches is ignored with one log line.",
                "For packs that change where content comes from, or want their own wording."})
        public String[] textOverrides = new String[0];
    }

    public static final class Plants {

        @Config.Name("sunpetal")
        @Config.Comment("Gives the Sunpetal Leaf. Grows on grass.")
        public final PlantValues sunpetal = new PlantValues(3, new String[] {"PLAINS", "SAVANNA"}, 0.5);

        @Config.Name("ironroot")
        @Config.Comment("Gives the Ironroot Sprig. Grows on grass or natural stone.")
        public final PlantValues ironroot = new PlantValues(3, new String[] {"MOUNTAIN", "HILLS"}, 0.5);

        @Config.Name("quickmint")
        @Config.Comment("Gives the Quickmint Leaf. Grows on grass or dirt.")
        public final PlantValues quickmint =
                new PlantValues(3, new String[] {"FOREST", "JUNGLE", "SWAMP"}, 0.5);

        @Config.Name("second_wind")
        @Config.Comment("Gives the Second Wind Petal. Grows on grass.")
        public final PlantValues secondWind =
                new PlantValues(1, new String[] {"FOREST", "PLAINS", "MAGICAL", "LUSH"}, 1.0);

        @Config.Name("everlasting_sprout")
        @Config.Comment("Gives the Everlasting Seed. Grows on grass or dirt.")
        public final PlantValues everlastingSprout = new PlantValues(3, new String[] {"*"}, 1.0);
    }

    public static final class PlantValues {

        @Config.Name("enabled")
        @Config.Comment("false stops this plant from generating; plants already in the world keep working.")
        public boolean enabled = true;

        @Config.Name("chancePercent")
        @Config.Comment({
                "Chance per new chunk, in percent, that one of this plant grows there, when the",
                "chunk has a matching biome and room on the right ground. 3 is 3 chunks in 100."})
        @Config.RangeInt(min = 0, max = 100)
        public int chancePercent;

        @Config.Name("biomeTypes")
        @Config.Comment({
                "Forge biome dictionary types this plant grows in, one per line, for example",
                "FOREST or SNOWY. * means every biome. An unknown type is ignored with one log line."})
        public String[] biomeTypes;

        @Config.Name("biomeNames")
        @Config.Comment({
                "Biome registry names this plant also grows in, one per line, for example",
                "minecraft:plains or a biome of another mod. A name nothing has is ignored with",
                "one log line."})
        public String[] biomeNames = new String[0];

        @Config.Name("regrowFactor")
        @Config.Comment("Multiplies general.plantRegrowMinutes for this plant. 0.5 is half the time.")
        @Config.RangeDouble(min = 0.1, max = 10.0)
        public double regrowFactor;

        PlantValues(int chancePercent, String[] biomeTypes, double regrowFactor) {
            this.chancePercent = chancePercent;
            this.biomeTypes = biomeTypes;
            this.regrowFactor = regrowFactor;
        }
    }

    public static final class Recipes {

        @Config.Name("common")
        public boolean common = true;

        @Config.Name("uncommon")
        public boolean uncommon = true;

        @Config.Name("rare")
        public boolean rare = true;

        // The infusion recipes are gone (owner decision 2026-08-25: infusions come from
        // treasure chests instead); only the Flask switches remain.
    }

    // Deliberately no ConfigChangedEvent handler. The approved contract, printed in every
    // comment above, is that values apply on the next game start; a live refresh would make an
    // in-game GUI edit take effect immediately and contradict the file the user just read.

    private FlaskConfig() {
    }
}
