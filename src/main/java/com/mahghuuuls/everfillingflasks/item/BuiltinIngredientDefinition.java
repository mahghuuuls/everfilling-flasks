package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.IngredientDefinition;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

/**
 * The definition behind each built-in ingredient. Like the tiers, kinds carry no numbers of
 * their own at runtime: cost and strength come from the config snapshot, so a pack author's
 * balance edits apply on the next game start.
 */
public final class BuiltinIngredientDefinition implements IngredientDefinition {

    private final IngredientKind kind;

    public BuiltinIngredientDefinition(IngredientKind kind) {
        this.kind = kind;
    }

    @Override
    public int potencyCost(ItemStack ingredient) {
        return ConfigSnapshot.current().ingredient(kind).cost();
    }

    /**
     * Where these herbs come from, and only while they actually come from there: a pack that
     * turns the loot injection off gets no hint rather than a false one, and can write its own
     * in the config (REQ-041).
     */
    @Override
    public String journalHint(ItemStack ingredient) {
        return ConfigSnapshot.current().ingredientLoot()
                ? "everfillingflasks.journal.hint.chests"
                : null;
    }

    @Override
    public void contribute(ItemStack ingredient, EntityPlayer player, FlaskBonuses bonuses) {
        float strength = (float) ConfigSnapshot.current().ingredient(kind).strength();
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
    public void onDrinkCompleted(ItemStack ingredient, ItemStack flask, EntityPlayer player) {
        if (kind.effect() == IngredientKind.Effect.POST_DRINK_REGEN) {
            int ticks = (int) Math.round(
                    ConfigSnapshot.current().ingredient(kind).strength() * 20.0);
            if (ticks > 0) {
                player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, ticks, 0));
            }
        }
    }
}
