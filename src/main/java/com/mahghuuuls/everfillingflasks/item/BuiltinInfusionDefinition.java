package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

/**
 * The definition behind each built-in infusion. Like the tiers, kinds carry no numbers of
 * their own at runtime: cost and strength come from the config snapshot, so a pack author's
 * balance edits apply on the next game start.
 */
public final class BuiltinInfusionDefinition implements InfusionDefinition {

    private final InfusionKind kind;

    public BuiltinInfusionDefinition(InfusionKind kind) {
        this.kind = kind;
    }

    @Override
    public int potencyCost(ItemStack infusion) {
        return ConfigSnapshot.current().infusion(kind).cost();
    }

    /**
     * The one place each built-in infusion's effect is put into words. The item tooltip and
     * the journal both read it here, so the two can never say different things, and the numbers
     * come from the same config the behaviour uses.
     */
    @Override
    public net.minecraft.util.text.ITextComponent effectDescription(ItemStack infusion) {
        ConfigSnapshot.InfusionConfig config = ConfigSnapshot.current().infusion(kind);
        int percent = (int) Math.round(config.strength() * 100.0);
        switch (kind.effect()) {
            case HEALING:
                return line("healing", percent);
            case HIT_THRESHOLD:
                return line("hitThreshold", percent);
            case DRINK_SPEED:
                return line("drinkSpeed", percent);
            case POST_DRINK_REGEN:
                return line("regen", (int) Math.round(config.strength()));
            default:
                return null;
        }
    }

    private static net.minecraft.util.text.ITextComponent line(String key, int value) {
        return new net.minecraft.util.text.TextComponentTranslation(
                "everfillingflasks.tooltip.infusion." + key, value);
    }

    /**
     * Where these herbs come from, and only while they actually come from there: a pack that
     * turns the loot injection off gets no hint rather than a false one, and can write its own
     * in the config (REQ-041).
     */
    @Override
    public String journalText(ItemStack infusion) {
        return ConfigSnapshot.current().infusionLoot()
                ? "everfillingflasks.journal.text.chests"
                : null;
    }

    @Override
    public void contribute(ItemStack infusion, EntityPlayer player, FlaskBonuses bonuses) {
        float strength = (float) ConfigSnapshot.current().infusion(kind).strength();
        switch (kind.effect()) {
            case HEALING:
                bonuses.healing(strength);
                break;
            case HIT_THRESHOLD:
                // The owner's player-facing term is "hit threshold"; the bonus type keeps its
                // original internal name.
                bonuses.hitResistance(strength);
                break;
            case DRINK_SPEED:
                bonuses.drinkSpeed(strength);
                break;
            case POST_DRINK_REGEN:
                // Nothing while placed; the petal acts in the completion hook.
                break;
        }
    }

    @Override
    public void onDrinkCompleted(ItemStack infusion, ItemStack flask, EntityPlayer player) {
        if (kind.effect() == InfusionKind.Effect.POST_DRINK_REGEN) {
            int ticks = (int) Math.round(
                    ConfigSnapshot.current().infusion(kind).strength() * 20.0);
            if (ticks > 0) {
                player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, ticks, 0));
            }
        }
    }
}
