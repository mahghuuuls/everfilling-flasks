package com.mahghuuuls.everfillingflasks.devfixtures;

import com.mahghuuuls.everfillingflasks.api.DrinkOutcome;
import com.mahghuuuls.everfillingflasks.api.FlaskApi;
import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;

/**
 * The 1.2.0 infusion API fixtures (campaign J), behind {@code -Deff.devfixtures.infusionapi}:
 *
 * <ul>
 * <li>Clay ball: declares a conflict with the brick; the brick declares nothing, so the pair
 * proves that one side saying so is enough (REQ-051).</li>
 * <li>Brick: +10% healing, no opinion about anything.</li>
 * <li>Glowstone dust: adds +25% effect power and overrides the four-argument hook, logging and
 * saying the effect power it received, which must equal the flask's own (REQ-052).</li>
 * <li>Tradeoff Herb (a fixture item that writes its own extra tooltip line): +20% healing and
 * -15% drink speed with no effect sentence, so its tooltip is generated (REQ-053).</li>
 * </ul>
 */
final class FixtureInfusions {

    private FixtureInfusions() {
    }

    static void register() {
        FlaskApi.registerInfusion(Items.CLAY_BALL, new ClayBall());
        FlaskApi.registerInfusion(Items.BRICK, new Brick());
        FlaskApi.registerInfusion(Items.GLOWSTONE_DUST, new Glowstone());
        FlaskApi.registerInfusion(DevItems.tradeoffHerb(), new TradeoffHerb());
        DevFixturesMod.LOGGER.info("Fixture infusion API active: clay ball conflicts with brick;"
                + " glowstone reads effect power; Tradeoff Herb has a generated tooltip");
    }

    static final class ClayBall implements InfusionDefinition {
        @Override
        public int potencyCost(ItemStack infusion) {
            return 1;
        }

        @Override
        public boolean conflictsWith(ItemStack self, ItemStack other) {
            return other.getItem() == Items.BRICK;
        }
    }

    static final class Brick implements InfusionDefinition {
        @Override
        public int potencyCost(ItemStack infusion) {
            return 1;
        }

        @Override
        public void contribute(ItemStack infusion, EntityPlayer player, FlaskBonuses bonuses) {
            bonuses.healing(0.10F);
        }
    }

    static final class Glowstone implements InfusionDefinition {
        @Override
        public int potencyCost(ItemStack infusion) {
            return 1;
        }

        @Override
        public void contribute(ItemStack infusion, EntityPlayer player, FlaskBonuses bonuses) {
            bonuses.effectPower(0.25F);
        }

        @Override
        public void onDrinkCompleted(ItemStack infusion, ItemStack flask, EntityPlayer player,
                                     DrinkOutcome outcome) {
            String line = String.format("infusion outcome: effectPower=%.2f",
                    outcome.effectPower());
            DevFixturesMod.LOGGER.info("{}: {}", player.getName(), line);
            player.sendMessage(new TextComponentString(line));
        }
    }

    static final class TradeoffHerb implements InfusionDefinition {
        @Override
        public int potencyCost(ItemStack infusion) {
            return 2;
        }

        @Override
        public void contribute(ItemStack infusion, EntityPlayer player, FlaskBonuses bonuses) {
            bonuses.healing(0.20F);
            bonuses.drinkSpeed(-0.15F);
        }
    }
}
