package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stat attributes and their conversion into flask bonuses: neutral at their starting values,
 * every vanilla operation read alike on a percentage stat, max charges rounded, and the ranges
 * holding at their limits.
 */
class FlaskAttributesTest {

    private static final float EPSILON = 1.0E-6F;

    private static AbstractAttributeMap playerMap() {
        AbstractAttributeMap map = new AttributeMap();
        FlaskAttributes.registerOn(map);
        return map;
    }

    private static FlaskBonuses bonusesOf(AbstractAttributeMap map) {
        FlaskBonuses bonuses = new FlaskBonuses();
        FlaskAttributes.addFrom(map, bonuses);
        return bonuses;
    }

    private static void modify(AbstractAttributeMap map, IAttribute attribute, double amount,
                               int operation) {
        map.getAttributeInstance(attribute).applyModifier(
                new AttributeModifier(UUID.randomUUID(), "test", amount, operation));
    }

    @Test
    void theAttributesAreNamedForTheirStats() {
        assertEquals("everfillingflasks.healing", FlaskAttributes.HEALING.getName());
        assertEquals("everfillingflasks.drinkSpeed", FlaskAttributes.DRINK_SPEED.getName());
        assertEquals("everfillingflasks.hitThreshold", FlaskAttributes.HIT_THRESHOLD.getName());
        assertEquals("everfillingflasks.rechargeSpeed", FlaskAttributes.RECHARGE_SPEED.getName());
        assertEquals("everfillingflasks.maxCharges", FlaskAttributes.MAX_CHARGES.getName());
        assertEquals("everfillingflasks.effectPower", FlaskAttributes.EFFECT_POWER.getName());
        for (IAttribute attribute : FlaskAttributes.all()) {
            assertTrue(attribute.getShouldWatch(), attribute.getName() + " reaches the client");
        }
    }

    @Test
    void aNewPlayerIsNeutral() {
        FlaskBonuses bonuses = bonusesOf(playerMap());
        assertEquals(0F, bonuses.healingSum(), EPSILON);
        assertEquals(0F, bonuses.drinkSpeedSum(), EPSILON);
        assertEquals(0F, bonuses.hitResistanceSum(), EPSILON);
        assertEquals(0F, bonuses.rechargeSpeedSum(), EPSILON);
        assertEquals(0, bonuses.maxChargesFlat());
        assertEquals(0F, bonuses.effectPowerSum(), EPSILON);
    }

    @Test
    void registeringTwiceChangesNothing() {
        AbstractAttributeMap map = playerMap();
        modify(map, FlaskAttributes.HEALING, 0.2D, 0);
        FlaskAttributes.registerOn(map);
        assertEquals(1.2D, map.getAttributeInstance(FlaskAttributes.HEALING).getAttributeValue(),
                EPSILON, "the existing instance and its modifier are kept");
    }

    @Test
    void eachChannelReadsItsOwnAttribute() {
        // Distinct amounts, so a swapped channel shows.
        AbstractAttributeMap map = playerMap();
        modify(map, FlaskAttributes.HEALING, 0.1D, 0);
        modify(map, FlaskAttributes.DRINK_SPEED, 0.2D, 0);
        modify(map, FlaskAttributes.HIT_THRESHOLD, 0.3D, 0);
        modify(map, FlaskAttributes.RECHARGE_SPEED, 0.4D, 0);
        modify(map, FlaskAttributes.MAX_CHARGES, 2D, 0);
        modify(map, FlaskAttributes.EFFECT_POWER, 0.5D, 0);
        FlaskBonuses bonuses = bonusesOf(map);
        assertEquals(0.1F, bonuses.healingSum(), EPSILON);
        assertEquals(0.2F, bonuses.drinkSpeedSum(), EPSILON);
        assertEquals(0.3F, bonuses.hitResistanceSum(), EPSILON);
        assertEquals(0.4F, bonuses.rechargeSpeedSum(), EPSILON);
        assertEquals(2, bonuses.maxChargesFlat());
        assertEquals(0.5F, bonuses.effectPowerSum(), EPSILON);
    }

    @Test
    void everyOperationMeansTheSameOnAPercentageStat() {
        for (int operation = 0; operation <= 2; operation++) {
            AbstractAttributeMap map = playerMap();
            modify(map, FlaskAttributes.HEALING, 0.2D, operation);
            assertEquals(0.2F, bonusesOf(map).healingSum(), EPSILON, "operation " + operation);
        }
    }

    @Test
    void theAttributeAddsToWhatTheAccumulatorAlreadyHolds() {
        AbstractAttributeMap map = playerMap();
        modify(map, FlaskAttributes.HEALING, 0.2D, 0);
        FlaskBonuses bonuses = new FlaskBonuses();
        bonuses.healing(0.08F);
        FlaskAttributes.addFrom(map, bonuses);
        assertEquals(0.28F, bonuses.healingSum(), EPSILON);
    }

    @Test
    void maxChargesRoundsToTheNearestWholeCharge() {
        assertEquals(0, chargesFor(0.4D));
        assertEquals(1, chargesFor(0.5D));
        assertEquals(1, chargesFor(0.6D));
        assertEquals(-1, chargesFor(-1D));
        assertEquals(3, chargesFor(2.7D));
    }

    private static int chargesFor(double value) {
        FlaskBonuses bonuses = new FlaskBonuses();
        FlaskAttributes.addTo(bonuses, 1D, 1D, 1D, 1D, value, 1D);
        return bonuses.maxChargesFlat();
    }

    @Test
    void aPercentageStatStopsAtMinusOneHundredPercent() {
        AbstractAttributeMap map = playerMap();
        modify(map, FlaskAttributes.DRINK_SPEED, -2D, 0);
        IAttributeInstance drinkSpeed = map.getAttributeInstance(FlaskAttributes.DRINK_SPEED);
        assertEquals(0D, drinkSpeed.getAttributeValue(), EPSILON);
        assertEquals(-1F, bonusesOf(map).drinkSpeedSum(), EPSILON);
    }

    @Test
    void theRangesHoldAtTheirLimits() {
        AbstractAttributeMap map = playerMap();
        modify(map, FlaskAttributes.HEALING, 5000D, 0);
        modify(map, FlaskAttributes.MAX_CHARGES, 500D, 0);
        assertEquals(1024D, map.getAttributeInstance(FlaskAttributes.HEALING).getAttributeValue(),
                EPSILON);
        assertEquals(64D, map.getAttributeInstance(FlaskAttributes.MAX_CHARGES).getAttributeValue(),
                EPSILON);
        modify(map, FlaskAttributes.MAX_CHARGES, -1000D, 0);
        assertEquals(-64D, map.getAttributeInstance(FlaskAttributes.MAX_CHARGES).getAttributeValue(),
                EPSILON);
    }
}
