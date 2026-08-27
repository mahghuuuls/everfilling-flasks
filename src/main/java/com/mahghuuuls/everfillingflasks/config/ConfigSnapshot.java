package com.mahghuuuls.everfillingflasks.config;

import com.mahghuuuls.everfillingflasks.item.FlaskTier;
import com.mahghuuuls.everfillingflasks.journal.JournalTextOverrides;
import com.mahghuuuls.everfillingflasks.item.InfusionKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The immutable values the rest of the mod reads. Built from {@link FlaskConfig} at class load
 * (the declared defaults) and rebuilt once at preInit after Forge has loaded the file. Nothing
 * rebuilds it later, so every reader sees one consistent set for the whole session and
 * clamping happens exactly once, with one warning per out-of-range key.
 *
 * <p>Forge's own range annotations already clamp at load; the clamps here are the same bounds
 * applied again so a value arriving by any other route still cannot escape them, and so the
 * warning names the key the user must fix.
 */
public final class ConfigSnapshot {

    private static volatile ConfigSnapshot current = build();

    private final String startingFlask;
    private final boolean keepFlaskOnDeath;
    private final float drinkSlowdown;
    private final boolean diagnostics;
    private final boolean infusionLoot;
    private final boolean inhibitedIntegration;
    private final JournalTextOverrides textOverrides;
    private final Map<FlaskTier, TierConfig> tiers;
    private final Map<InfusionKind, InfusionConfig> infusions;
    private final Map<String, Boolean> recipes;
    private final List<String> clampWarnings;

    private ConfigSnapshot(String startingFlask, boolean keepFlaskOnDeath, float drinkSlowdown,
                           boolean diagnostics, boolean infusionLoot,
                           boolean inhibitedIntegration,
                           JournalTextOverrides textOverrides,
                           Map<FlaskTier, TierConfig> tiers,
                           Map<InfusionKind, InfusionConfig> infusions,
                           Map<String, Boolean> recipes, List<String> clampWarnings) {
        this.startingFlask = startingFlask;
        this.keepFlaskOnDeath = keepFlaskOnDeath;
        this.drinkSlowdown = drinkSlowdown;
        this.diagnostics = diagnostics;
        this.infusionLoot = infusionLoot;
        this.inhibitedIntegration = inhibitedIntegration;
        this.textOverrides = textOverrides;
        this.tiers = tiers;
        this.infusions = infusions;
        this.recipes = recipes;
        this.clampWarnings = clampWarnings;
    }

    /** The values as they stood when the configuration was last read. */
    public static ConfigSnapshot current() {
        return current;
    }

    /** Reads {@link FlaskConfig} into a fresh snapshot. Called once, at preInit. */
    public static void refresh() {
        current = build();
    }

    /** The one mapping from {@link FlaskConfig} fields to snapshot values. */
    private static ConfigSnapshot build() {
        List<String> warnings = new ArrayList<String>();
        Map<FlaskTier, TierConfig> tiers = new EnumMap<FlaskTier, TierConfig>(FlaskTier.class);
        tiers.put(FlaskTier.COMMON, TierConfig.from("flasks.common", FlaskConfig.flasks.common, warnings));
        tiers.put(FlaskTier.UNCOMMON, TierConfig.from("flasks.uncommon", FlaskConfig.flasks.uncommon, warnings));
        tiers.put(FlaskTier.RARE, TierConfig.from("flasks.rare", FlaskConfig.flasks.rare, warnings));

        Map<InfusionKind, InfusionConfig> infusions =
                new EnumMap<InfusionKind, InfusionConfig>(InfusionKind.class);
        infusions.put(InfusionKind.SUNPETAL_LEAF, InfusionConfig.from(
                "infusions.sunpetalLeaf", FlaskConfig.infusions.sunpetalLeaf, warnings));
        infusions.put(InfusionKind.IRONROOT_SPRIG, InfusionConfig.from(
                "infusions.ironrootSprig", FlaskConfig.infusions.ironrootSprig, warnings));
        infusions.put(InfusionKind.QUICKMINT_LEAF, InfusionConfig.from(
                "infusions.quickmintLeaf", FlaskConfig.infusions.quickmintLeaf, warnings));
        infusions.put(InfusionKind.SECOND_WIND_PETAL, InfusionConfig.from(
                "infusions.secondWindPetal", FlaskConfig.infusions.secondWindPetal, warnings));

        // Keyed by the same lowercase names the recipe JSON conditions use, so one condition
        // class covers every switchable recipe; only the Flasks have recipes now.
        Map<String, Boolean> recipes = new HashMap<String, Boolean>();
        recipes.put(FlaskTier.COMMON.key(), FlaskConfig.recipes.common);
        recipes.put(FlaskTier.UNCOMMON.key(), FlaskConfig.recipes.uncommon);
        recipes.put(FlaskTier.RARE.key(), FlaskConfig.recipes.rare);

        JournalTextOverrides textOverrides =
                JournalTextOverrides.parse(FlaskConfig.journal.textOverrides);
        warnings.addAll(textOverrides.warnings());

        return new ConfigSnapshot(
                FlaskConfig.general.startingFlask == null ? "" : FlaskConfig.general.startingFlask.trim(),
                FlaskConfig.general.keepFlaskOnDeath,
                (float) clampDouble(FlaskConfig.general.drinkSlowdown, 0.0, 1.0,
                        "general.drinkSlowdown", warnings),
                FlaskConfig.general.diagnostics,
                FlaskConfig.general.infusionLoot,
                FlaskConfig.general.inhibitedIntegration,
                textOverrides,
                Collections.unmodifiableMap(tiers),
                Collections.unmodifiableMap(infusions),
                Collections.unmodifiableMap(recipes),
                Collections.unmodifiableList(warnings));
    }

    public String startingFlask() {
        return startingFlask;
    }

    public boolean keepFlaskOnDeath() {
        return keepFlaskOnDeath;
    }

    public float drinkSlowdown() {
        return drinkSlowdown;
    }

    public boolean diagnostics() {
        return diagnostics;
    }

    /** Whether the built-in infusions join the vanilla treasure chest loot. */
    /** A pack author's replacements for journal entry text (REQ-041). */
    public JournalTextOverrides textOverrides() {
        return textOverrides;
    }

    /** Whether the Inhibited effect is allowed to freeze recharge (REQ-012). */
    public boolean inhibitedIntegration() {
        return inhibitedIntegration;
    }

    public boolean infusionLoot() {
        return infusionLoot;
    }

    public TierConfig tier(FlaskTier tier) {
        return tiers.get(tier);
    }

    public InfusionConfig infusion(InfusionKind kind) {
        return infusions.get(kind);
    }

    public boolean recipeEnabled(FlaskTier tier) {
        return recipeEnabled(tier.key());
    }

    /**
     * The switch for one named recipe, tier or infusion. An unknown name reads as enabled:
     * a typo in a recipe file must not silently remove content, and the mismatch is visible
     * in the config file the name fails to match.
     */
    public boolean recipeEnabled(String name) {
        Boolean enabled = recipes.get(name);
        return enabled == null || enabled;
    }

    /** One line per clamped key, for the preInit warning pass. Empty when everything was in range. */
    public List<String> clampWarnings() {
        return clampWarnings;
    }

    static int clampInt(int value, int min, int max, String key, List<String> warnings) {
        if (value < min || value > max) {
            int clamped = Math.max(min, Math.min(max, value));
            warnings.add(key + " = " + value + " is outside " + min + ".." + max
                    + "; using " + clamped);
            return clamped;
        }
        return value;
    }

    static double clampDouble(double value, double min, double max, String key, List<String> warnings) {
        if (value < min || value > max) {
            double clamped = Math.max(min, Math.min(max, value));
            warnings.add(key + " = " + value + " is outside " + min + ".." + max
                    + "; using " + clamped);
            return clamped;
        }
        return value;
    }

    /** One tier's clamped values. */
    public static final class TierConfig {

        private final int maxCharges;
        private final float healPercentage;
        private final int rechargeTicks;
        private final int drinkTicks;
        private final float hitThreshold;
        private final int potency;

        TierConfig(int maxCharges, float healPercentage, int rechargeTicks, int drinkTicks,
                   float hitThreshold, int potency) {
            this.maxCharges = maxCharges;
            this.healPercentage = healPercentage;
            this.rechargeTicks = rechargeTicks;
            this.drinkTicks = drinkTicks;
            this.hitThreshold = hitThreshold;
            this.potency = potency;
        }

        static TierConfig from(String keyPrefix, FlaskConfig.TierValues values, List<String> warnings) {
            return new TierConfig(
                    clampInt(values.maxCharges, 1, 64, keyPrefix + ".maxCharges", warnings),
                    (float) clampDouble(values.healPercentage, 0.0, 1.0,
                            keyPrefix + ".healPercentage", warnings),
                    clampInt(values.rechargeTicks, 1, 72000, keyPrefix + ".rechargeTicks", warnings),
                    clampInt(values.drinkTicks, 1, 1200, keyPrefix + ".drinkTicks", warnings),
                    (float) clampDouble(values.hitThreshold, 0.0, 1000.0,
                            keyPrefix + ".hitThreshold", warnings),
                    clampInt(values.potency, 0, 1000, keyPrefix + ".potency", warnings));
        }

        public int maxCharges() {
            return maxCharges;
        }

        public float healPercentage() {
            return healPercentage;
        }

        public int rechargeTicks() {
            return rechargeTicks;
        }

        public int drinkTicks() {
            return drinkTicks;
        }

        public float hitThreshold() {
            return hitThreshold;
        }

        public int potency() {
            return potency;
        }
    }

    /** One infusion's clamped values. */
    public static final class InfusionConfig {

        private final int cost;
        private final double strength;

        InfusionConfig(int cost, double strength) {
            this.cost = cost;
            this.strength = strength;
        }

        static InfusionConfig from(String keyPrefix, FlaskConfig.InfusionValues values,
                                     List<String> warnings) {
            return new InfusionConfig(
                    clampInt(values.cost, 0, 1000, keyPrefix + ".cost", warnings),
                    clampDouble(values.strength, 0.0, 100.0, keyPrefix + ".strength", warnings));
        }

        public int cost() {
            return cost;
        }

        public double strength() {
            return strength;
        }
    }
}
