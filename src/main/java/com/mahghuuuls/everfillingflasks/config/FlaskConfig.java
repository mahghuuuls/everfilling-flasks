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
    @Config.Comment("The four built-in Flask tiers. Times are in ticks; 20 ticks are 1 second.")
    public static final Flasks flasks = new Flasks();

    @Config.Name("recipes")
    @Config.Comment("One switch per built-in recipe. false removes that recipe entirely.")
    public static final Recipes recipes = new Recipes();

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

        @Config.Name("diagnostics")
        @Config.Comment({
                "Log one line per Flask decision: drink started, refused (and why), cancelled",
                "(and why), completed, charge restored, recharge paused. For pack authors and",
                "server owners investigating reports. Off for normal play."})
        public boolean diagnostics = false;
    }

    public static final class Flasks {

        @Config.Name("common")
        public final TierValues common = new TierValues(1);

        @Config.Name("uncommon")
        public final TierValues uncommon = new TierValues(2);

        @Config.Name("rare")
        public final TierValues rare = new TierValues(3);

        @Config.Name("epic")
        public final TierValues epic = new TierValues(4);
    }

    public static final class TierValues {

        @Config.Name("maxCharges")
        @Config.Comment("Charges this Flask holds when full.")
        @Config.RangeInt(min = 1, max = 64)
        public int maxCharges;

        @Config.Name("healPercentage")
        @Config.Comment("Health restored per drink, as a fraction of maximum health. 0.3 is 30 percent.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double healPercentage = 0.30;

        @Config.Name("rechargeTicks")
        @Config.Comment("Ticks to restore one missing charge. 1200 is one minute.")
        @Config.RangeInt(min = 1, max = 72000)
        public int rechargeTicks = 1200;

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

    public static final class Recipes {

        @Config.Name("common")
        public boolean common = true;

        @Config.Name("uncommon")
        public boolean uncommon = true;

        @Config.Name("rare")
        public boolean rare = true;

        @Config.Name("epic")
        public boolean epic = true;
    }

    // Deliberately no ConfigChangedEvent handler. The approved contract, printed in every
    // comment above, is that values apply on the next game start; a live refresh would make an
    // in-game GUI edit take effect immediately and contradict the file the user just read.

    private FlaskConfig() {
    }
}
