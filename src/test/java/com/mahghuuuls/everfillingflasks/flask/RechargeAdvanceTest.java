package com.mahghuuuls.everfillingflasks.flask;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bulk recharge push (REQ-048) at its boundaries, and its agreement with the per-tick step:
 * pushing N ticks must land exactly where N single ticks would.
 */
class RechargeAdvanceTest {

    @Test
    void aPartialPushMovesProgressAndKeepsCharges() {
        FlaskMechanics.AdvanceResult r = FlaskMechanics.advanceBy(100, 200, 600, 1, 3);
        assertEquals(300, r.progress());
        assertEquals(1, r.charges());
        assertEquals(200, r.ticksApplied());
    }

    @Test
    void aPushAcrossTwoChargesCarriesTheRemainder() {
        // 500 completes the first charge, the remaining 600 exactly complete the second.
        FlaskMechanics.AdvanceResult r = FlaskMechanics.advanceBy(100, 1100, 600, 1, 3);
        assertEquals(3, r.charges());
        assertEquals(0, r.progress());
        assertEquals(1100, r.ticksApplied());
    }

    @Test
    void ticksPastFullAreNotApplied() {
        // 100 needed to fill; the other 900 are refused, not swallowed.
        FlaskMechanics.AdvanceResult r = FlaskMechanics.advanceBy(500, 1000, 600, 2, 3);
        assertEquals(3, r.charges());
        assertEquals(0, r.progress());
        assertEquals(100, r.ticksApplied());
    }

    @Test
    void aFullFlaskOrNoTicksAppliesNothing() {
        FlaskMechanics.AdvanceResult full = FlaskMechanics.advanceBy(0, 500, 600, 3, 3);
        assertEquals(0, full.ticksApplied());
        assertEquals(3, full.charges());
        assertEquals(0, full.progress());

        FlaskMechanics.AdvanceResult none = FlaskMechanics.advanceBy(100, 0, 600, 1, 3);
        assertEquals(0, none.ticksApplied());
        assertEquals(100, none.progress());

        FlaskMechanics.AdvanceResult negative = FlaskMechanics.advanceBy(100, -5, 600, 1, 3);
        assertEquals(0, negative.ticksApplied());
    }

    @Test
    void aZeroTickRechargeCompletesOneChargePerTickLikeTheStepDoes() {
        // advance() rolls over every tick when rechargeTicks <= 1; the push must agree and
        // must not spin: 5 ticks, 2 charges missing, 2 applied.
        FlaskMechanics.AdvanceResult r = FlaskMechanics.advanceBy(0, 5, 0, 1, 3);
        assertEquals(3, r.charges());
        assertEquals(2, r.ticksApplied());
    }

    @Test
    void aPushLandsWhereTheSameNumberOfSingleTicksWould() {
        int[][] cases = {
                {100, 200, 600, 1, 3}, {100, 1100, 600, 1, 3}, {500, 1000, 600, 2, 3},
                {599, 1, 600, 0, 1}, {0, 37, 12, 0, 5}, {7, 1000, 12, 0, 5}, {0, 3, 1, 0, 2},
                {0, 5, 0, 1, 3}};
        for (int[] c : cases) {
            int progress = c[0];
            int charges = c[3];
            int applied = 0;
            for (int i = 0; i < c[1] && charges < c[4]; i++) {
                FlaskMechanics.RechargeStep step = FlaskMechanics.advance(progress, c[2]);
                progress = step.progress();
                applied++;
                if (step.chargeGained()) {
                    charges = FlaskMechanics.clampCharges(charges + 1, c[4]);
                }
            }
            FlaskMechanics.AdvanceResult r = FlaskMechanics.advanceBy(c[0], c[1], c[2], c[3], c[4]);
            String label = java.util.Arrays.toString(c);
            assertEquals(progress, r.progress(), "progress " + label);
            assertEquals(charges, r.charges(), "charges " + label);
            assertEquals(applied, r.ticksApplied(), "applied " + label);
        }
    }
}
