package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tooltip formatter (REQ-053): signs always shown, whole percentages rounded half away
 * from zero, zeros dropped, a fixed channel order, charges as a signed count.
 */
class BonusDescriptionsTest {

    private static String render(List<BonusDescriptions.Line> lines) {
        StringBuilder text = new StringBuilder();
        for (BonusDescriptions.Line line : lines) {
            text.append(line.value()).append(' ').append(line.channel()).append('|');
        }
        return text.toString();
    }

    @Test
    void aTradeOffReadsWithBothSigns() {
        FlaskBonuses b = new FlaskBonuses();
        b.healing(0.20F);
        b.drinkSpeed(-0.15F);
        assertEquals("+20% healing|-15% drinkSpeed|", render(BonusDescriptions.describe(b)));
    }

    @Test
    void roundingIsHalfAwayFromZeroAndZerosAreDropped() {
        FlaskBonuses b = new FlaskBonuses();
        // 0.125 is exact in float, so 12.5 is a true half: half away from zero gives 13 on both
        // sides; banker's rounding would give 12, and rounding the signed value would give -12.
        b.healing(0.125F);
        b.drinkSpeed(-0.125F);
        b.hitResistance(0.004F);
        b.rechargeSpeed(-0.004F);
        // And "+0%" lines would appear if zeros were kept.
        assertEquals("+13% healing|-13% drinkSpeed|", render(BonusDescriptions.describe(b)));
    }

    @Test
    void theOrderIsFixedWhateverTheCallOrder() {
        FlaskBonuses b = new FlaskBonuses();
        b.effectPower(0.5F);
        b.maxCharges(2);
        b.rechargeSpeed(1.0F);
        b.hitResistance(0.32F);
        b.drinkSpeed(0.16F);
        b.healing(0.08F);
        assertEquals("+8% healing|+16% drinkSpeed|+32% hitThreshold|+100% rechargeSpeed|"
                        + "+2 maxCharges|+50% effectPower|",
                render(BonusDescriptions.describe(b)));
    }

    @Test
    void oneChargeIsSingularAndALossIsSigned() {
        FlaskBonuses plus = new FlaskBonuses();
        plus.maxCharges(1);
        assertEquals("+1 maxCharge|", render(BonusDescriptions.describe(plus)));
        FlaskBonuses minus = new FlaskBonuses();
        minus.maxCharges(-2);
        assertEquals("-2 maxCharges|", render(BonusDescriptions.describe(minus)));
    }

    @Test
    void anEmptyContributionHasNoLines() {
        assertTrue(BonusDescriptions.describe(new FlaskBonuses()).isEmpty());
    }
}
