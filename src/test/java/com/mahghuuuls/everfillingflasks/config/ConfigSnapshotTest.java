package com.mahghuuuls.everfillingflasks.config;

import com.mahghuuuls.everfillingflasks.item.FlaskTier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clamping produces the bounded value and names the offending key; in-range values pass through
 * silently; the built-in defaults match the approved requirement numbers.
 */
class ConfigSnapshotTest {

    @Test
    void outOfRangeIntIsClampedWithOneWarningNamingTheKey() {
        List<String> warnings = new ArrayList<String>();
        int clamped = ConfigSnapshot.clampInt(0, 1, 72000, "flasks.common.rechargeTicks", warnings);
        assertEquals(1, clamped);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("flasks.common.rechargeTicks"));
    }

    @Test
    void inRangeValuesPassSilently() {
        List<String> warnings = new ArrayList<String>();
        assertEquals(1200, ConfigSnapshot.clampInt(1200, 1, 72000, "k", warnings));
        assertEquals(0.5, ConfigSnapshot.clampDouble(0.5, 0.0, 1.0, "k", warnings), 1.0E-9);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void outOfRangeDoubleIsClampedWithAWarning() {
        List<String> warnings = new ArrayList<String>();
        assertEquals(1.0, ConfigSnapshot.clampDouble(2.5, 0.0, 1.0, "flasks.epic.healPercentage",
                warnings), 1.0E-9);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("flasks.epic.healPercentage"));
    }

    @Test
    void tierConfigClampsEveryField() {
        List<String> warnings = new ArrayList<String>();
        FlaskConfig.TierValues values = new FlaskConfig.TierValues(0);
        values.healPercentage = -0.5;
        values.rechargeTicks = 0;
        values.drinkTicks = 100000;
        values.hitThreshold = -1.0;
        ConfigSnapshot.TierConfig tier = ConfigSnapshot.TierConfig.from("flasks.test", values, warnings);
        assertEquals(1, tier.maxCharges());
        assertEquals(0.0F, tier.healPercentage(), 1.0E-6F);
        assertEquals(1, tier.rechargeTicks());
        assertEquals(1200, tier.drinkTicks());
        assertEquals(0.0F, tier.hitThreshold(), 1.0E-6F);
        assertEquals(5, warnings.size());
    }

    @Test
    void refreshReadsTheDeclaredDefaultsFromFlaskConfig() {
        // Through the real path: FlaskConfig's declared defaults -> refresh() -> snapshot.
        // The shipped defaults are load-bearing: nobody edits them, so only this test names them.
        ConfigSnapshot.refresh();
        ConfigSnapshot defaults = ConfigSnapshot.current();
        ConfigSnapshot.TierConfig common = defaults.tier(FlaskTier.COMMON);
        assertEquals(1, common.maxCharges());
        assertEquals(0.30F, common.healPercentage(), 1.0E-6F);
        assertEquals(1200, common.rechargeTicks());
        assertEquals(30, common.drinkTicks());
        assertEquals(1.0F, common.hitThreshold(), 1.0E-6F);
        assertEquals(2, defaults.tier(FlaskTier.UNCOMMON).maxCharges());
        assertEquals(3, defaults.tier(FlaskTier.RARE).maxCharges());
        assertEquals(4, defaults.tier(FlaskTier.EPIC).maxCharges());
        assertTrue(defaults.recipeEnabled(FlaskTier.COMMON));
        assertTrue(defaults.recipeEnabled(FlaskTier.RARE));
        assertEquals("everfillingflasks:common_flask", defaults.startingFlask());
        assertTrue(defaults.keepFlaskOnDeath());
        assertEquals(0.5F, defaults.drinkSlowdown(), 1.0E-6F);
        assertFalse(defaults.diagnostics());
        assertTrue(defaults.clampWarnings().isEmpty());
    }

    @Test
    void refreshClampsAnEditedValueAndNamesItsRealKey() {
        int original = FlaskConfig.flasks.common.rechargeTicks;
        try {
            FlaskConfig.flasks.common.rechargeTicks = 0;
            ConfigSnapshot.refresh();
            ConfigSnapshot snapshot = ConfigSnapshot.current();
            assertEquals(1, snapshot.tier(FlaskTier.COMMON).rechargeTicks());
            assertEquals(1, snapshot.clampWarnings().size());
            // The warning must name the key as it appears in the file, produced by refresh()
            // itself, not by a key string this test supplies.
            assertTrue(snapshot.clampWarnings().get(0).contains("flasks.common.rechargeTicks"));
        } finally {
            FlaskConfig.flasks.common.rechargeTicks = original;
            ConfigSnapshot.refresh();
        }
    }
}
