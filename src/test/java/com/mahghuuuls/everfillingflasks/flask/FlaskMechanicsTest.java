package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The approved formulas, each pinned by the mutation that would break it: swapping the
 * multiply for a divide, dropping a clamp, or moving the rollover boundary by one tick all
 * fail exactly one assertion here.
 */
class FlaskMechanicsTest {

    private static FlaskBonuses bonuses() {
        return new FlaskBonuses();
    }

    @Test
    void percentageBonusesOfOneKindAddThenMultiply() {
        FlaskBonuses b = bonuses();
        b.healing(0.5F);
        b.healing(0.3F);
        EffectiveFlask e = FlaskMechanics.effective(4, 0.30F, 1200, 30, 1.0F, b);
        // 0.30 * (1 + 0.8); a divide or a compounding multiply would not give 0.54.
        assertEquals(0.54F, e.healPercentage(), 1.0E-6F);
    }

    @Test
    void drinkSpeedDividesTheDuration() {
        FlaskBonuses b = bonuses();
        b.drinkSpeed(1.0F);
        assertEquals(15, FlaskMechanics.effective(4, 0.3F, 1200, 30, 1.0F, b).drinkTicks());
    }

    @Test
    void drinkDurationNeverDropsBelowTheFloor() {
        FlaskBonuses b = bonuses();
        b.drinkSpeed(10.0F);
        assertEquals(FlaskMechanics.MIN_DRINK_TICKS,
                FlaskMechanics.effective(4, 0.3F, 1200, 30, 1.0F, b).drinkTicks());
    }

    @Test
    void hitResistanceMultipliesTheThreshold() {
        FlaskBonuses b = bonuses();
        b.hitResistance(1.0F);
        assertEquals(2.0F, FlaskMechanics.effective(4, 0.3F, 1200, 30, 1.0F, b).hitThreshold(), 1.0E-6F);
    }

    @Test
    void rechargeSpeedDividesTheRechargeTime() {
        FlaskBonuses b = bonuses();
        b.rechargeSpeed(1.0F);
        assertEquals(600, FlaskMechanics.effective(4, 0.3F, 1200, 30, 1.0F, b).rechargeTicks());
    }

    @Test
    void maxChargesAddFlatAndNeverDropBelowOne() {
        FlaskBonuses plus = bonuses();
        plus.maxCharges(1);
        assertEquals(3, FlaskMechanics.effective(2, 0.3F, 1200, 30, 1.0F, plus).maxCharges());

        FlaskBonuses minus = bonuses();
        minus.maxCharges(-10);
        assertEquals(FlaskMechanics.MIN_MAX_CHARGES,
                FlaskMechanics.effective(2, 0.3F, 1200, 30, 1.0F, minus).maxCharges());
    }

    @Test
    void aNegativeSumNeverProducesANegativeMultiplier() {
        FlaskBonuses b = bonuses();
        b.healing(-3.0F);
        // Multiplier clamps at 0; healing cannot go negative and hurt the player.
        assertEquals(0.0F, FlaskMechanics.effective(4, 0.3F, 1200, 30, 1.0F, b).healPercentage(), 1.0E-6F);
    }

    @Test
    void fullyNegativeSpeedSaturatesTheDuration() {
        // Multiplier 0 on a duration must saturate, not throw or produce zero.
        FlaskBonuses b = bonuses();
        b.drinkSpeed(-5.0F);
        assertEquals(Integer.MAX_VALUE,
                FlaskMechanics.effective(4, 0.3F, 1200, 30, 1.0F, b).drinkTicks());
    }

    @Test
    void rechargeSpeedNeverProducesAZeroTickRecharge() {
        FlaskBonuses b = bonuses();
        b.rechargeSpeed(10000.0F);
        // Math.round(1200 / 10001) is 0; without the floor a charge would arrive every tick.
        assertEquals(FlaskMechanics.MIN_RECHARGE_TICKS,
                FlaskMechanics.effective(4, 0.3F, 1200, 30, 1.0F, b).rechargeTicks());
    }

    @Test
    void rolloverHappensExactlyAtTheThresholdAndResetsProgress() {
        FlaskMechanics.RechargeStep rollover = FlaskMechanics.advance(1199, 1200);
        assertTrue(rollover.chargeGained());
        assertEquals(0, rollover.progress());

        FlaskMechanics.RechargeStep plain = FlaskMechanics.advance(500, 1200);
        assertFalse(plain.chargeGained());
        assertEquals(501, plain.progress());

        assertFalse(FlaskMechanics.advance(1198, 1200).chargeGained());
    }

    @Test
    void chargesClampIntoTheEffectiveRange() {
        assertEquals(2, FlaskMechanics.clampCharges(3, 2));
        assertEquals(0, FlaskMechanics.clampCharges(-1, 2));
        assertEquals(2, FlaskMechanics.clampCharges(2, 4));
    }

    @Test
    void onlyAttackerDamageAtOrAboveTheThresholdInterrupts() {
        assertTrue(FlaskMechanics.interrupts(true, 1.0F, 1.0F));
        assertTrue(FlaskMechanics.interrupts(true, 2.0F, 1.0F));
        assertFalse(FlaskMechanics.interrupts(true, 0.5F, 1.0F));
        assertFalse(FlaskMechanics.interrupts(false, 100.0F, 1.0F));
        // Threshold 0: any real hit cancels, including a 0.0 hit that armor absorbed entirely.
        assertTrue(FlaskMechanics.interrupts(true, 0.0F, 0.0F));
    }

    @Test
    void healAmountIsMaxHealthTimesPercentage() {
        assertEquals(6.0F, FlaskMechanics.healAmount(20.0F, 0.30F), 1.0E-6F);
    }

    @Test
    void drinkStartRequiresAValidFlaskWithACharge() {
        org.junit.jupiter.api.Assertions.assertFalse(
                FlaskMechanics.canStartDrink(false, 4, false, 0.3F, 10.0F, 20.0F));
        org.junit.jupiter.api.Assertions.assertFalse(
                FlaskMechanics.canStartDrink(true, 0, false, 0.3F, 10.0F, 20.0F));
        org.junit.jupiter.api.Assertions.assertFalse(
                FlaskMechanics.canStartDrink(true, 4, true, 0.3F, 10.0F, 20.0F));
        org.junit.jupiter.api.Assertions.assertTrue(
                FlaskMechanics.canStartDrink(true, 1, false, 0.3F, 10.0F, 20.0F));
    }

    @Test
    void fullHealthBlocksOnlyFlasksThatHeal() {
        // The zero-heal exemption: a pure-hook Flask works at full health.
        org.junit.jupiter.api.Assertions.assertFalse(
                FlaskMechanics.canStartDrink(true, 4, false, 0.3F, 20.0F, 20.0F));
        org.junit.jupiter.api.Assertions.assertTrue(
                FlaskMechanics.canStartDrink(true, 4, false, 0.0F, 20.0F, 20.0F));
    }

    @Test
    void interpolationAddsElapsedTicksToTheLastKnownProgress() {
        // Would fail if the addition were dropped or the arguments swapped.
        assertEquals(150, FlaskMechanics.interpolateProgress(100, 50, false, false, 1200));
    }

    @Test
    void interpolationFreezesWhilePaused() {
        // Would fail if the paused flag were ignored and elapsed ticks still added.
        assertEquals(100, FlaskMechanics.interpolateProgress(100, 50, true, false, 1200));
    }

    @Test
    void interpolationFreezesAtMaximumCharges() {
        assertEquals(0, FlaskMechanics.interpolateProgress(0, 500, false, true, 1200));
    }

    @Test
    void interpolationNeverClaimsAChargeTheServerHasNotGranted() {
        // Caps at one tick short of the threshold; a cap at the threshold itself would let a
        // display show a full icon the server may still deny.
        assertEquals(1199, FlaskMechanics.interpolateProgress(1150, 500, false, false, 1200));
    }
}
