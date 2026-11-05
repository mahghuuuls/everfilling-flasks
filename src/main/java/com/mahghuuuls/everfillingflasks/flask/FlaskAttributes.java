package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.Tags;
import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.FlaskModifierSource;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The six flask stats as player attributes, so any mod that changes player attributes by name
 * (item qualities, set bonuses, food effects) changes a player's flasks. They are also the core's
 * own modifier source: the final value of each, after every modifier on it, joins the flask
 * bonuses like any other source's.
 *
 * <p>The percentage stats and effect power start at 1.0, a multiplier, so every vanilla operation
 * means what it says: adding 0.2, or multiplying the base or the total by 0.2, are all +20
 * percent. They never go below 0, which is -100 percent. Max charges starts at 0 and counts whole
 * charges, rounded to the nearest one.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class FlaskAttributes implements FlaskModifierSource {

    private static final double PERCENT_DEFAULT = 1.0D;
    private static final double PERCENT_MIN = 0.0D;
    private static final double PERCENT_MAX = 1024.0D;
    private static final double CHARGES_LIMIT = 64.0D;

    // Display names come from the language file (attribute.name.<name>), which is what vanilla
    // and the mods that show attributes read.
    public static final IAttribute HEALING = percent("healing");
    public static final IAttribute DRINK_SPEED = percent("drinkSpeed");
    public static final IAttribute HIT_THRESHOLD = percent("hitThreshold");
    public static final IAttribute RECHARGE_SPEED = percent("rechargeSpeed");
    public static final IAttribute MAX_CHARGES = new RangedAttribute(null, name("maxCharges"), 0.0D,
            -CHARGES_LIMIT, CHARGES_LIMIT).setShouldWatch(true);
    public static final IAttribute EFFECT_POWER = percent("effectPower");

    private static final List<IAttribute> ALL = Collections.unmodifiableList(Arrays.asList(
            HEALING, DRINK_SPEED, HIT_THRESHOLD, RECHARGE_SPEED, MAX_CHARGES, EFFECT_POWER));

    /** The core's own player modifier source. */
    public static final FlaskModifierSource SOURCE = new FlaskAttributes();

    private FlaskAttributes() {
    }

    private static String name(String stat) {
        return Tags.MOD_ID + "." + stat;
    }

    private static IAttribute percent(String stat) {
        return new RangedAttribute(null, name(stat), PERCENT_DEFAULT, PERCENT_MIN, PERCENT_MAX)
                .setShouldWatch(true);
    }

    public static List<IAttribute> all() {
        return ALL;
    }

    /**
     * Adds the attributes to every player as it is created, on both sides. The attribute map is
     * created on first use, so this runs before saved modifiers are read back and before vanilla
     * adds its own.
     */
    @SubscribeEvent
    public static void onConstructing(EntityEvent.EntityConstructing event) {
        if (event.getEntity() instanceof EntityPlayer) {
            registerOn(((EntityPlayer) event.getEntity()).getAttributeMap());
        }
    }

    /** Registers each attribute the map does not have yet; calling it again changes nothing. */
    static void registerOn(AbstractAttributeMap map) {
        for (IAttribute attribute : ALL) {
            if (map.getAttributeInstance(attribute) == null) {
                map.registerAttribute(attribute);
            }
        }
    }

    @Override
    public void contribute(EntityPlayer player, FlaskBonuses bonuses) {
        addFrom(player.getAttributeMap(), bonuses);
    }

    /** The final values in {@code map}, after every modifier, added to {@code bonuses}. */
    static void addFrom(AbstractAttributeMap map, FlaskBonuses bonuses) {
        addTo(bonuses, value(map, HEALING), value(map, DRINK_SPEED), value(map, HIT_THRESHOLD),
                value(map, RECHARGE_SPEED), value(map, MAX_CHARGES), value(map, EFFECT_POWER));
    }

    private static double value(AbstractAttributeMap map, IAttribute attribute) {
        // Every player gets the attributes at construction. If one is missing anyway, the throw
        // reaches the modifier registry, which logs it once and keeps the other sources.
        return map.getAttributeInstance(attribute).getAttributeValue();
    }

    /**
     * The conversion: a percentage stat's value minus its starting 1.0 is its bonus, and max
     * charges adds its value rounded to the nearest whole charge, halves rounding up.
     */
    static void addTo(FlaskBonuses bonuses, double healing, double drinkSpeed, double hitThreshold,
                      double rechargeSpeed, double maxCharges, double effectPower) {
        bonuses.healing((float) (healing - PERCENT_DEFAULT));
        bonuses.drinkSpeed((float) (drinkSpeed - PERCENT_DEFAULT));
        bonuses.hitResistance((float) (hitThreshold - PERCENT_DEFAULT));
        bonuses.rechargeSpeed((float) (rechargeSpeed - PERCENT_DEFAULT));
        bonuses.maxCharges((int) Math.round(maxCharges));
        bonuses.effectPower((float) (effectPower - PERCENT_DEFAULT));
    }
}
