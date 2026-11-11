package com.mahghuuuls.everfillingflasks.plant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The regrow clock: minutes of world time times 1200 ticks times the plant's factor, counted
 * from the pick, and a stem that blooms only once the clock has reached its deadline.
 */
class PlantLifecycleTest {

    @Test
    void aFullFactorAtTheDefaultMinutesIsThreeHoursOfTicks() {
        assertEquals(180L * 1200L, PlantLifecycle.regrowTicks(180, 1.0D));
    }

    @Test
    void halfTheFactorHalvesTheTime() {
        assertEquals(90L * 1200L, PlantLifecycle.regrowTicks(180, 0.5D));
    }

    @Test
    void theSmallestSettingsStillGiveWholeTicks() {
        assertEquals(120L, PlantLifecycle.regrowTicks(1, 0.1D));
    }

    @Test
    void ticksAreRoundedToTheNearestWholeTickNotCutOff() {
        // 180 x 1200 x 2.3 is 496799.99999999994 in double arithmetic.
        assertEquals(496800L, PlantLifecycle.regrowTicks(180, 2.3D));
    }

    @Test
    void aPickReadsTheRegrowTimeAndFactorFromTheConfiguration() {
        int minutes = com.mahghuuuls.everfillingflasks.config.FlaskConfig.general.plantRegrowMinutes;
        double factor = com.mahghuuuls.everfillingflasks.config.FlaskConfig.plants.quickmint.regrowFactor;
        try {
            com.mahghuuuls.everfillingflasks.config.FlaskConfig.general.plantRegrowMinutes = 2;
            com.mahghuuuls.everfillingflasks.config.FlaskConfig.plants.quickmint.regrowFactor = 0.25D;
            com.mahghuuuls.everfillingflasks.config.ConfigSnapshot.refresh();
            assertEquals(1000L + 600L, PlantLifecycle.deadlineFor(WildPlant.QUICKMINT, 1000L));
        } finally {
            com.mahghuuuls.everfillingflasks.config.FlaskConfig.general.plantRegrowMinutes = minutes;
            com.mahghuuuls.everfillingflasks.config.FlaskConfig.plants.quickmint.regrowFactor = factor;
            com.mahghuuuls.everfillingflasks.config.ConfigSnapshot.refresh();
        }
    }

    @Test
    void theDeadlineCountsFromThePick() {
        assertEquals(5000L + 180L * 1200L, PlantLifecycle.regrowAt(5000L, 180, 1.0D));
    }

    @Test
    void aStemStaysAStemBeforeItsDeadline() {
        assertEquals(PlantStage.STEM, PlantLifecycle.decide(PlantStage.STEM, 1000L, 999L));
    }

    @Test
    void aStemBloomsAtItsDeadline() {
        assertEquals(PlantStage.BLOOM, PlantLifecycle.decide(PlantStage.STEM, 1000L, 1000L));
    }

    @Test
    void aStemLongPastItsDeadlineBloomsAtItsNextTick() {
        assertEquals(PlantStage.BLOOM, PlantLifecycle.decide(PlantStage.STEM, 1000L, 1000000L));
    }

    @Test
    void aStemWithoutADeadlineBloomsRatherThanNever() {
        assertEquals(PlantStage.BLOOM, PlantLifecycle.decide(PlantStage.STEM, Long.MIN_VALUE, 0L));
    }

    @Test
    void aBloomStaysABloom() {
        assertEquals(PlantStage.BLOOM, PlantLifecycle.decide(PlantStage.BLOOM, 1000L, 0L));
    }

    @Test
    void onlyTheEverlastingSproutSparkles() {
        for (WildPlant plant : WildPlant.values()) {
            assertEquals(plant == WildPlant.EVERLASTING_SPROUT, plant.sparkles(), plant + " sparkles");
        }
    }

    @Test
    void onlyTheEverlastingSproutInBloomGivesOffLight() {
        assertEquals(5, PlantLifecycle.lightFor(WildPlant.EVERLASTING_SPROUT, PlantStage.BLOOM));
        assertEquals(0, PlantLifecycle.lightFor(WildPlant.EVERLASTING_SPROUT, PlantStage.STEM));
        for (WildPlant plant : WildPlant.values()) {
            if (plant != WildPlant.EVERLASTING_SPROUT) {
                assertEquals(0, PlantLifecycle.lightFor(plant, PlantStage.BLOOM), plant + " bloom");
                assertEquals(0, PlantLifecycle.lightFor(plant, PlantStage.STEM), plant + " stem");
            }
        }
    }

    @Test
    void theMetadataOfEachStageIsFixed() {
        assertEquals(0, PlantStage.BLOOM.ordinal());
        assertEquals(1, PlantStage.STEM.ordinal());
        assertEquals(PlantStage.STEM, PlantStage.fromMeta(1));
        assertEquals(PlantStage.BLOOM, PlantStage.fromMeta(0));
        assertEquals(PlantStage.BLOOM, PlantStage.fromMeta(7));
    }
}
