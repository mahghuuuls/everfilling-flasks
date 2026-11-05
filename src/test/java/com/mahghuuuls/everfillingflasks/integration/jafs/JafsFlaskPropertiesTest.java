package com.mahghuuuls.everfillingflasks.integration.jafs;

import com.mahghuuuls.everfillingflasks.flask.FlaskAttributes;
import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The food properties and their applier, without JAFS running: the declared ids, operations and
 * ranges, and what a pair of sums does to the flask attribute.
 */
class JafsFlaskPropertiesTest {

    private static final double EPSILON = 1.0E-9D;

    private static AbstractAttributeMap playerMap() {
        AbstractAttributeMap map = new AttributeMap();
        for (IAttribute attribute : FlaskAttributes.all()) {
            map.registerAttribute(attribute);
        }
        return map;
    }

    private static IAttributeInstance instance(AbstractAttributeMap map,
                                               JafsFlaskProperties.Stat stat) {
        return map.getAttributeInstance(stat.attribute);
    }

    @Test
    void thePropertiesAreNamedAndBoundedAsDocumented() {
        String[] ids = {"everfillingflasks:healing", "everfillingflasks:drink_speed",
                "everfillingflasks:hit_threshold", "everfillingflasks:recharge_speed",
                "everfillingflasks:max_charges", "everfillingflasks:effect_power"};
        JafsFlaskProperties.Stat[] stats = JafsFlaskProperties.Stat.values();
        assertEquals(ids.length, stats.length);
        for (int i = 0; i < stats.length; i++) {
            PropertySpec spec = stats[i].spec();
            assertEquals(ids[i], spec.id());
            assertEquals("attribute.name." + stats[i].attribute.getName(), spec.displayNameKey());
            if (stats[i] == JafsFlaskProperties.Stat.MAX_CHARGES) {
                assertTrue(spec.supports(PropertyOperation.FLAT));
                assertFalse(spec.supports(PropertyOperation.PERCENT));
                assertEquals(-10D, spec.range(PropertyOperation.FLAT).min(), EPSILON);
                assertEquals(10D, spec.range(PropertyOperation.FLAT).max(), EPSILON);
            } else {
                assertTrue(spec.supports(PropertyOperation.PERCENT));
                assertFalse(spec.supports(PropertyOperation.FLAT));
                assertEquals(-100D, spec.range(PropertyOperation.PERCENT).min(), EPSILON);
                assertEquals(1000D, spec.range(PropertyOperation.PERCENT).max(), EPSILON);
            }
        }
    }

    @Test
    void eachPropertyMovesItsOwnAttribute() {
        assertEquals(FlaskAttributes.HEALING, JafsFlaskProperties.Stat.HEALING.attribute);
        assertEquals(FlaskAttributes.DRINK_SPEED, JafsFlaskProperties.Stat.DRINK_SPEED.attribute);
        assertEquals(FlaskAttributes.HIT_THRESHOLD, JafsFlaskProperties.Stat.HIT_THRESHOLD.attribute);
        assertEquals(FlaskAttributes.RECHARGE_SPEED,
                JafsFlaskProperties.Stat.RECHARGE_SPEED.attribute);
        assertEquals(FlaskAttributes.MAX_CHARGES, JafsFlaskProperties.Stat.MAX_CHARGES.attribute);
        assertEquals(FlaskAttributes.EFFECT_POWER, JafsFlaskProperties.Stat.EFFECT_POWER.attribute);
    }

    @Test
    void twentyPercentIsAPointTwoAddModifier() {
        AbstractAttributeMap map = playerMap();
        JafsFlaskProperties.apply(map, JafsFlaskProperties.Stat.HEALING, 0D, 20D);
        IAttributeInstance healing = instance(map, JafsFlaskProperties.Stat.HEALING);
        assertEquals(1.2D, healing.getAttributeValue(), EPSILON);
        assertEquals(1, healing.getModifiers().size());
        for (AttributeModifier modifier : healing.getModifiers()) {
            // Add, so it sums with other sources; a multiply would read the same alone.
            assertEquals(0, modifier.getOperation());
            assertEquals(0.2D, modifier.getAmount(), EPSILON);
        }
    }

    @Test
    void oneFlatChargeIsOneCharge() {
        AbstractAttributeMap map = playerMap();
        JafsFlaskProperties.apply(map, JafsFlaskProperties.Stat.MAX_CHARGES, 1D, 0D);
        assertEquals(1D, instance(map, JafsFlaskProperties.Stat.MAX_CHARGES).getAttributeValue(),
                EPSILON);
    }

    @Test
    void theSameSumsAgainLeaveOneModifierAndNewSumsReplaceIt() {
        AbstractAttributeMap map = playerMap();
        JafsFlaskProperties.Stat stat = JafsFlaskProperties.Stat.DRINK_SPEED;
        JafsFlaskProperties.apply(map, stat, 0D, 20D);
        JafsFlaskProperties.apply(map, stat, 0D, 20D);
        assertEquals(1, instance(map, stat).getModifiers().size());
        assertEquals(1.2D, instance(map, stat).getAttributeValue(), EPSILON);
        JafsFlaskProperties.apply(map, stat, 0D, 50D);
        assertEquals(1, instance(map, stat).getModifiers().size());
        assertEquals(1.5D, instance(map, stat).getAttributeValue(), EPSILON);
    }

    @Test
    void zeroSumsRemoveTheModifier() {
        AbstractAttributeMap map = playerMap();
        JafsFlaskProperties.Stat stat = JafsFlaskProperties.Stat.EFFECT_POWER;
        JafsFlaskProperties.apply(map, stat, 0D, 30D);
        JafsFlaskProperties.apply(map, stat, 0D, 0D);
        assertTrue(instance(map, stat).getModifiers().isEmpty());
        assertEquals(1D, instance(map, stat).getAttributeValue(), EPSILON);
    }

    @Test
    void theModifierIsNotSavedWithThePlayer() {
        AbstractAttributeMap map = playerMap();
        JafsFlaskProperties.Stat stat = JafsFlaskProperties.Stat.RECHARGE_SPEED;
        JafsFlaskProperties.apply(map, stat, 0D, 10D);
        assertEquals(1, instance(map, stat).getModifiers().size());
        for (AttributeModifier modifier : instance(map, stat).getModifiers()) {
            assertFalse(modifier.isSaved());
            assertEquals(JafsFlaskProperties.MODIFIER_NAME, modifier.getName());
        }
    }

    @Test
    void anOffLinkRemovesWhatItWroteInsteadOfWriting() {
        AbstractAttributeMap map = playerMap();
        JafsFlaskProperties.Stat stat = JafsFlaskProperties.Stat.HEALING;
        JafsFlaskProperties.push(map, stat, 0D, 20D);
        assertEquals(1.2D, instance(map, stat).getAttributeValue(), EPSILON, "written while on");
        try {
            JafsFlaskProperties.setEnabledForTests(false);
            JafsFlaskProperties.push(map, stat, 0D, 40D);
            assertTrue(instance(map, stat).getModifiers().isEmpty());
        } finally {
            JafsFlaskProperties.setEnabledForTests(true);
        }
    }

    @Test
    void aFailingPushTurnsTheLinkOffAndClearsTheMap() {
        // A map without the drink-speed attribute makes that push fail; the healing modifier
        // written before must go with it.
        AbstractAttributeMap map = new AttributeMap();
        map.registerAttribute(FlaskAttributes.HEALING);
        JafsFlaskProperties.push(map, JafsFlaskProperties.Stat.HEALING, 0D, 20D);
        assertEquals(1.2D, map.getAttributeInstance(FlaskAttributes.HEALING).getAttributeValue(),
                EPSILON, "written while on");
        try {
            JafsFlaskProperties.push(map, JafsFlaskProperties.Stat.DRINK_SPEED, 0D, 20D);
            assertFalse(JafsFlaskProperties.enabled());
            assertTrue(map.getAttributeInstance(FlaskAttributes.HEALING).getModifiers().isEmpty());
        } finally {
            JafsFlaskProperties.setEnabledForTests(true);
        }
    }

    @Test
    void anotherModifierOnTheSameAttributeIsLeftAlone() {
        AbstractAttributeMap map = playerMap();
        JafsFlaskProperties.Stat stat = JafsFlaskProperties.Stat.HIT_THRESHOLD;
        instance(map, stat).applyModifier(new AttributeModifier("quality", 0.1D, 0));
        JafsFlaskProperties.apply(map, stat, 0D, 20D);
        JafsFlaskProperties.apply(map, stat, 0D, 0D);
        assertEquals(1.1D, instance(map, stat).getAttributeValue(), EPSILON);
    }
}
