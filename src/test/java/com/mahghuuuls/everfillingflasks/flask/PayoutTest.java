package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The over-time heal formulas (REQ-047): the shares of a payout sum to its total exactly, the
 * over-time fraction takes the same healing multiplier as the instant part, and the declared
 * values are clamped into their ranges.
 */
class PayoutTest {

    /** Runs a payout to the end the way the controller does and returns the total paid. */
    private static float payAll(float total, int ticks) {
        float remaining = total;
        float paid = 0.0F;
        for (int left = ticks; left > 0; left--) {
            float share = FlaskMechanics.payoutStep(remaining, left);
            paid += share;
            remaining -= share;
        }
        assertEquals(0.0F, remaining, 1.0E-6F, "nothing left owed");
        return paid;
    }

    @Test
    void sharesSumToTheTotal() {
        // 4 points over 100 ticks: 0.04 per tick does not divide cleanly in float.
        assertEquals(4.0F, payAll(4.0F, 100), 1.0E-4F);
        // 1 point over 3 ticks: a third is not representable at all.
        assertEquals(1.0F, payAll(1.0F, 3), 1.0E-6F);
        // One tick: everything at once.
        assertEquals(6.6F, payAll(6.6F, 1), 1.0E-6F);
    }

    @Test
    void everyShareIsPositiveAndTheLastPaysTheRemainder() {
        float remaining = 4.0F;
        for (int left = 100; left > 1; left--) {
            float share = FlaskMechanics.payoutStep(remaining, left);
            assertTrue(share > 0.0F, "tick " + left);
            remaining -= share;
        }
        // Division by one is exact in float, so the last share is the remainder to the bit.
        assertEquals(remaining, FlaskMechanics.payoutStep(remaining, 1), 0.0F);
    }

    @Test
    void nothingOwedOrNoTicksPaysNothing() {
        assertEquals(0.0F, FlaskMechanics.payoutStep(0.0F, 10), 0.0F);
        assertEquals(0.0F, FlaskMechanics.payoutStep(4.0F, 0), 0.0F);
        assertEquals(0.0F, FlaskMechanics.payoutStep(-1.0F, 10), 0.0F);
    }

    @Test
    void theOverTimeFractionTakesTheHealingMultiplier() {
        FlaskBonuses b = new FlaskBonuses();
        b.healing(0.5F);
        EffectiveFlask e = FlaskMechanics.effective(2, 0.1F, 600, 30, 1.0F, 0.2F, 100, b);
        // Same multiplier as the instant part; 0.2 would mean the bonus was skipped, 0.45 that
        // it was applied twice.
        assertEquals(0.3F, e.healOverTimePercentage(), 1.0E-6F);
        assertEquals(0.15F, e.healPercentage(), 1.0E-6F);
        assertEquals(100, e.healOverTimeTicks());
    }

    @Test
    void theSixArgumentFormDeclaresNoOverTimePart() {
        EffectiveFlask e = FlaskMechanics.effective(2, 0.3F, 600, 30, 1.0F, new FlaskBonuses());
        assertEquals(0.0F, e.healOverTimePercentage(), 0.0F);
        assertEquals(0, e.healOverTimeTicks());
    }

    @Test
    void declaredValuesAreClamped() {
        FlaskBonuses none = new FlaskBonuses();
        EffectiveFlask high = FlaskMechanics.effective(2, 0.3F, 600, 30, 1.0F, 5.0F, 999999, none);
        assertEquals(1.0F, high.healOverTimePercentage(), 1.0E-6F);
        assertEquals(FlaskMechanics.MAX_PAYOUT_TICKS, high.healOverTimeTicks());
        EffectiveFlask low = FlaskMechanics.effective(2, 0.3F, 600, 30, 1.0F, -1.0F, -5, none);
        assertEquals(0.0F, low.healOverTimePercentage(), 0.0F);
        assertEquals(0, low.healOverTimeTicks());
    }
}
