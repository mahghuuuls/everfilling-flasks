package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * The definition behind each built-in tier. The four tiers carry no numbers of their own; every
 * value comes from the config snapshot, which is read once at preInit, so a pack author's file
 * is the truth for the session and edits apply on the next game start.
 */
public final class TierFlaskDefinition implements FlaskDefinition {

    private final FlaskTier tier;

    public TierFlaskDefinition(FlaskTier tier) {
        this.tier = tier;
    }

    @Override
    public int maxCharges(ItemStack stack, EntityPlayer player) {
        return ConfigSnapshot.current().tier(tier).maxCharges();
    }

    @Override
    public float healPercentage(ItemStack stack, EntityPlayer player) {
        return ConfigSnapshot.current().tier(tier).healPercentage();
    }

    @Override
    public int rechargeTicks(ItemStack stack, EntityPlayer player) {
        return ConfigSnapshot.current().tier(tier).rechargeTicks();
    }

    @Override
    public int drinkTicks(ItemStack stack, EntityPlayer player) {
        return ConfigSnapshot.current().tier(tier).drinkTicks();
    }

    @Override
    public float hitThreshold(ItemStack stack, EntityPlayer player) {
        return ConfigSnapshot.current().tier(tier).hitThreshold();
    }

    @Override
    public int potency(ItemStack stack, EntityPlayer player) {
        return ConfigSnapshot.current().tier(tier).potency();
    }
}
