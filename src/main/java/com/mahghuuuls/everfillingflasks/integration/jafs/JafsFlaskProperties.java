package com.mahghuuuls.everfillingflasks.integration.jafs;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.flask.FlaskAttributes;
import com.mahghuuuls.jafs.api.IPropertyApplier;
import com.mahghuuuls.jafs.api.JafsApi;
import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The six Just Another Food System food properties that change the flask stats. JAFS sums a
 * player's active foods per property and hands the sums to the applier here, which turns them into
 * one modifier on the matching flask attribute; the attribute then reaches the flask like any
 * other. Nothing outside this package names a JAFS type, and this class is reached only when JAFS
 * is loaded.
 *
 * <p>The modifier is not saved with the player: JAFS pushes the sums again at login and respawn,
 * so a pack that later drops JAFS leaves nothing behind on its players.
 */
public final class JafsFlaskProperties {

    static final String MODIFIER_NAME = "JAFS";

    /** One property, the attribute it moves, and the amounts a food may give it. */
    enum Stat {
        HEALING("healing", FlaskAttributes.HEALING, "red"),
        DRINK_SPEED("drink_speed", FlaskAttributes.DRINK_SPEED, "aqua"),
        HIT_THRESHOLD("hit_threshold", FlaskAttributes.HIT_THRESHOLD, "gray"),
        RECHARGE_SPEED("recharge_speed", FlaskAttributes.RECHARGE_SPEED, "yellow"),
        MAX_CHARGES("max_charges", FlaskAttributes.MAX_CHARGES, "gold"),
        EFFECT_POWER("effect_power", FlaskAttributes.EFFECT_POWER, "light_purple");

        // Whole percent for the multipliers (20 is +20 percent); whole charges for max charges.
        private static final double PERCENT_MIN = -100.0D;
        private static final double PERCENT_MAX = 1000.0D;
        private static final double CHARGES_MIN = -10.0D;
        private static final double CHARGES_MAX = 10.0D;

        final String id;
        final IAttribute attribute;
        final String color;
        final UUID modifierId;

        Stat(String property, IAttribute attribute, String color) {
            this.id = "everfillingflasks:" + property;
            this.attribute = attribute;
            this.color = color;
            this.modifierId = UUID.nameUUIDFromBytes(
                    ("everfillingflasks:jafs:" + property).getBytes(StandardCharsets.UTF_8));
        }

        boolean countsCharges() {
            return this == MAX_CHARGES;
        }

        PropertySpec spec() {
            PropertySpec.Builder builder = PropertySpec.builder(id)
                    .displayNameKey("attribute.name." + attribute.getName())
                    .color(color);
            if (countsCharges()) {
                builder.operation(PropertyOperation.FLAT, CHARGES_MIN, CHARGES_MAX);
            } else {
                builder.operation(PropertyOperation.PERCENT, PERCENT_MIN, PERCENT_MAX);
            }
            return builder.build();
        }

        /** The attribute modifier amount for JAFS's sums; each property takes one operation. */
        double amount(double flatSum, double percentSum) {
            return countsCharges() ? flatSum : percentSum / 100.0D;
        }
    }

    private static volatile boolean enabled = true;

    private JafsFlaskProperties() {
    }

    /**
     * Registers the six properties; a failure turns the link off with one warning. Every spec is
     * built before the first registration, so a spec that cannot be built registers none.
     */
    public static void register() {
        try {
            List<PropertySpec> specs = new ArrayList<PropertySpec>();
            for (Stat stat : Stat.values()) {
                specs.add(stat.spec());
            }
            for (int i = 0; i < specs.size(); i++) {
                JafsApi.registerProperty(specs.get(i), applier(Stat.values()[i]));
            }
        } catch (RuntimeException | LinkageError failure) {
            disable(failure);
        }
    }

    private static IPropertyApplier applier(final Stat stat) {
        return (EntityPlayerMP player, double flatSum, double percentSum) ->
                push(player.getAttributeMap(), stat, flatSum, percentSum);
    }

    /**
     * What the applier does with JAFS's sums: writes them while the link is on, removes this
     * link's modifiers once it is off, and turns the link off on a failure.
     */
    static void push(AbstractAttributeMap map, Stat stat, double flatSum, double percentSum) {
        try {
            if (enabled) {
                apply(map, stat, flatSum, percentSum);
            } else {
                apply(map, stat, 0.0D, 0.0D);
            }
        } catch (RuntimeException | LinkageError failure) {
            disable(failure);
            clearFrom(map);
        }
    }

    /** Removes every modifier this link wrote on the map; best effort. */
    static void clearFrom(AbstractAttributeMap map) {
        for (Stat stat : Stat.values()) {
            try {
                apply(map, stat, 0.0D, 0.0D);
            } catch (RuntimeException | LinkageError ignored) {
                // Unsaved, so whatever is left is gone at the player's next login.
            }
        }
    }

    /**
     * Writes the sums as this link's one modifier on the stat's attribute: the old one is removed
     * first, so the same sums again change nothing, and zero sums leave none.
     */
    static void apply(AbstractAttributeMap map, Stat stat, double flatSum, double percentSum) {
        IAttributeInstance instance = map.getAttributeInstance(stat.attribute);
        instance.removeModifier(stat.modifierId);
        double amount = stat.amount(flatSum, percentSum);
        if (amount != 0.0D) {
            instance.applyModifier(new AttributeModifier(stat.modifierId, MODIFIER_NAME, amount, 0)
                    .setSaved(false));
        }
    }

    /** Turns the link off, logs once, and takes its modifiers off every player online. */
    private static void disable(Throwable failure) {
        if (!enabled) {
            return;
        }
        enabled = false;
        EverfillingFlasksMod.LOGGER.warn("The Just Another Food System link failed and is now off;"
                + " foods no longer change flask stats this session", failure);
        try {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                    clearFrom(player.getAttributeMap());
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // No server to walk; each player's next push or login clears the rest.
        }
    }

    static boolean enabled() {
        return enabled;
    }

    /** Test seam: the flag is static and a test that turns the link off must turn it back on. */
    static void setEnabledForTests(boolean value) {
        enabled = value;
    }
}
