package com.mahghuuuls.everfillingflasks.devfixtures;

import com.mahghuuuls.everfillingflasks.api.FlaskApi;
import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.FlaskModifierSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The development fixture add-on: a separate mod, in a separate source set, packaged in a
 * separate jar that only the development client loads. It exercises the public API exactly the
 * way a third-party mod would, and none of it can reach the shipped artifact.
 *
 * <p>Registered fixtures, each behind a system property so a card enables only what it tests:
 *
 * <ul>
 * <li>{@code -Deff.devfixtures.modifiers=true}: while the player holds a stick in the off-hand,
 * +50 percent healing, +100 percent drink speed, +100 percent hit resistance, +100 percent
 * recharge speed, and +1 maximum charge.</li>
 * <li>{@code -Deff.devfixtures.throwing=true}: one source that always throws, for watching the
 * isolation contain it.</li>
 * </ul>
 *
 * <p>Registration happens in init, one phase after the core mod's preInit, deliberately: it
 * proves late registration works the way the API promises.
 */
@Mod(modid = DevFixturesMod.MOD_ID, name = "Everfilling Flasks Dev Fixtures", version = "0.0.0",
        dependencies = "required-after:everfillingflasks")
public class DevFixturesMod {

    public static final String MOD_ID = "everfillingflasksdev";
    static final Logger LOGGER = LogManager.getLogger("Everfilling Flasks Dev Fixtures");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("eff.devfixtures.modifiers")) {
            FlaskApi.registerModifierSource(new StickBonuses());
            LOGGER.info("Fixture modifier source active: off-hand stick grants flask bonuses");
        }
        if (Boolean.getBoolean("eff.devfixtures.throwing")) {
            FlaskApi.registerModifierSource(new AlwaysThrows());
            LOGGER.info("Fixture throwing modifier source active");
        }
    }

    /** The well-behaved fixture: bonuses exist exactly while the off-hand holds a stick. */
    static final class StickBonuses implements FlaskModifierSource {

        @Override
        public void contribute(EntityPlayer player, FlaskBonuses bonuses) {
            if (player.getHeldItemOffhand().getItem() == Items.STICK) {
                bonuses.healing(0.5F);
                bonuses.drinkSpeed(1.0F);
                bonuses.hitResistance(1.0F);
                bonuses.rechargeSpeed(1.0F);
                bonuses.maxCharges(1);
            }
        }
    }

    /** The hostile fixture: proves one broken add-on costs only its own bonuses. */
    static final class AlwaysThrows implements FlaskModifierSource {

        @Override
        public void contribute(EntityPlayer player, FlaskBonuses bonuses) {
            throw new IllegalStateException("dev fixture: this modifier source always fails");
        }
    }
}
