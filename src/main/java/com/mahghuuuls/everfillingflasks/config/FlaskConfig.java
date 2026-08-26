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

    @Config.Name("ingredients")
    @Config.Comment({
            "The four built-in Flask Ingredients. cost is potency used per placed piece.",
            "strength is the effect size: a fraction for the percentage ingredients",
            "(0.10 is +10 percent per piece), seconds for the Second Wind Petal."})
    public static final Ingredients ingredients = new Ingredients();

    @Config.Name("recipes")
    @Config.Comment("One switch per built-in recipe. false removes that recipe entirely.")
    public static final Recipes recipes = new Recipes();

    @Config.Name("journal")
    @Config.Comment("The in-game journal. These options change words only, never behaviour.")
    public static final Journal journal = new Journal();

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

        @Config.Name("ingredientLoot")
        @Config.Comment({
                "Add Flask Ingredients to dungeon, mineshaft, and village blacksmith chests.",
                "false removes them from world loot entirely."})
        public boolean ingredientLoot = true;

        @Config.Name("diagnostics")
        @Config.Comment({
                "Log one line per Flask decision: drink started, refused (and why), cancelled",
                "(and why), completed, charge restored, recharge paused. For pack authors and",
                "server owners investigating reports. Off for normal play."})
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
                "Ingredient budget of this Flask's infusion grid. Placed ingredients whose",
                "summed costs exceed it make the Flask unusable until pieces are removed.",
                "Every tier shares the same default on purpose: tiers differ by charges."})
        @Config.RangeInt(min = 0, max = 1000)
        public int potency = 10;

        TierValues(int maxCharges) {
            this.maxCharges = maxCharges;
        }
    }

    public static final class Ingredients {

        @Config.Name("sunpetalLeaf")
        @Config.Comment("+healing per piece; strength 0.10 is +10 percent.")
        public final IngredientValues sunpetalLeaf = new IngredientValues(2, 0.10);

        @Config.Name("ironrootSprig")
        @Config.Comment("+hit threshold per piece; strength 0.40 is +40 percent.")
        public final IngredientValues ironrootSprig = new IngredientValues(2, 0.40);

        @Config.Name("quickmintLeaf")
        @Config.Comment("+drink speed per piece; strength 0.20 is +20 percent.")
        public final IngredientValues quickmintLeaf = new IngredientValues(2, 0.20);

        @Config.Name("secondWindPetal")
        @Config.Comment("Regeneration after a completed drink; strength is seconds.")
        public final IngredientValues secondWindPetal = new IngredientValues(3, 5.0);
    }

    public static final class IngredientValues {

        @Config.Name("cost")
        @Config.Comment("Potency used per placed piece.")
        @Config.RangeInt(min = 0, max = 1000)
        public int cost;

        @Config.Name("strength")
        @Config.Comment("Effect size; see the ingredient's own comment for its unit.")
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double strength;

        IngredientValues(int cost, double strength) {
            this.cost = cost;
            this.strength = strength;
        }
    }

    public static final class Journal {

        @Config.Name("hintOverrides")
        @Config.Comment({
                "Replace or hide a journal's \"Where to Find\" line, one entry per line.",
                "everfillingflasks:sunpetal_leaf=Traded by wandering herbalists. replaces it;",
                "everfillingflasks:sunpetal_leaf= (nothing after the equals sign) hides it.",
                "Use the item's registry name, never its display name, so the setting survives",
                "a language change. A name nothing matches is ignored with one log line.",
                "For packs that change where content comes from."})
        public String[] hintOverrides = new String[0];
    }

    public static final class Recipes {

        @Config.Name("common")
        public boolean common = true;

        @Config.Name("uncommon")
        public boolean uncommon = true;

        @Config.Name("rare")
        public boolean rare = true;

        // The ingredient recipes are gone (owner decision 2026-08-25: ingredients come from
        // treasure chests instead); only the Flask switches remain.
    }

    // Deliberately no ConfigChangedEvent handler. The approved contract, printed in every
    // comment above, is that values apply on the next game start; a live refresh would make an
    // in-game GUI edit take effect immediately and contradict the file the user just read.

    private FlaskConfig() {
    }
}
