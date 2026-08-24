package com.mahghuuuls.everfillingflasks.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * What makes an item a Flask: its numbers, and optionally what else happens when a drink
 * completes. Register one per {@link net.minecraft.item.Item} through {@link FlaskApi}.
 *
 * <p>Every method receives the exact stack and the player, so values may depend on NBT or on
 * the player. The core calls these on the logical server; keep them cheap and free of side
 * effects, because they are consulted whenever effective values are computed.
 *
 * <p>Values are bases. Player Flask modifiers are applied by the core afterwards; a definition
 * must not try to include them.
 */
public interface FlaskDefinition {

    /** Base maximum charges. Values below 1 are treated as 1. */
    int maxCharges(ItemStack stack, EntityPlayer player);

    /**
     * Base healing, as a fraction of the player's maximum health, 0.0 to 1.0. A Flask with 0
     * relies entirely on {@link #onDrinkCompleted} and may be drunk at full health.
     */
    float healPercentage(ItemStack stack, EntityPlayer player);

    /** Base ticks to restore one missing charge. Values below 1 are treated as 1. */
    int rechargeTicks(ItemStack stack, EntityPlayer player);

    /** Base ticks the key must be held for one drink. Values below 1 are treated as 1. */
    int drinkTicks(ItemStack stack, EntityPlayer player);

    /**
     * Base hit threshold in half-hearts. Damage from an attacker at or above the effective
     * threshold cancels the drink. 0 means any hit cancels.
     */
    float hitThreshold(ItemStack stack, EntityPlayer player);

    /**
     * Potency: the ingredient budget of this Flask's infusion grid. Placed ingredients whose
     * summed costs exceed it make the Flask unusable until pieces are removed. Values below 0
     * are treated as 0; 0 means no ingredient fits. The built-in tiers all default to 10.
     */
    default int potency(ItemStack stack, EntityPlayer player) {
        return 10;
    }

    /**
     * Called on the logical server after a drink completes, the charge is spent, and the
     * standard healing is applied. Never called for a cancelled drink. A thrown exception is
     * caught and logged by the core; it cannot corrupt Flask or player state, and it cannot
     * cancel or repeat any core step.
     */
    default void onDrinkCompleted(ItemStack stack, EntityPlayer player) {
    }

    /**
     * Completion presentation, particle half. Called on the logical server when a drink
     * completes, before {@link #onDrinkCompleted}. Return {@code true} for the core's default
     * burst around the drinker; return {@code false} to disable it, or spawn a replacement
     * here first and then return {@code false}. A thrown exception is caught and logged and
     * the default plays.
     */
    default boolean completionEffect(ItemStack stack, EntityPlayer player) {
        return true;
    }

    /**
     * Completion presentation, sound half, with the same contract as
     * {@link #completionEffect}: {@code true} for the core's default chime, {@code false} to
     * disable or after playing a replacement.
     */
    default boolean completionSound(ItemStack stack, EntityPlayer player) {
        return true;
    }
}
