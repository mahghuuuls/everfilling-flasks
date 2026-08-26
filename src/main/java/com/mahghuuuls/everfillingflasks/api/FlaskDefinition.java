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
     * Potency: the infusion budget of this Flask's infusion grid. Placed infusions whose
     * summed costs exceed it make the Flask unusable until pieces are removed. Values below 0
     * are treated as 0; 0 means no infusion fits. The built-in tiers all default to 10.
     */
    default int potency(ItemStack stack, EntityPlayer player) {
        return 10;
    }

    /**
     * How many infusion slots this Flask has, from 1 to 12. Six by default, which is what every
     * Flask had before this existed.
     *
     * <p>Slots and {@link #potency} are independent on purpose: a Flask may offer many slots and
     * a small budget, or few slots and a large one, and those are different Flasks to play with.
     *
     * <p>The stack alone decides, with no player, because a Flask's stored infusions are read in
     * places where there is no player to ask. A value outside the range is clamped and reported
     * once.
     */
    default int infusionSlots(ItemStack stack) {
        return 6;
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

    /**
     * The liquid color of this Flask in the default HUD's charge icons, as 0xRRGGBB. Return
     * -1 for the built-in red. Read on the client with the synced stack, once per frame at
     * most; keep it cheap. A throw is logged once and the default color is used.
     */
    default int hudLiquidColor(ItemStack stack, EntityPlayer player) {
        return -1;
    }

    /**
     * A full replacement for the default HUD's glass icon (the empty-charge look), drawn at
     * nine by nine points. Null keeps the built-in. Client-read like
     * {@link #hudLiquidColor}, with the same isolation.
     */
    default net.minecraft.util.ResourceLocation hudGlassTexture(ItemStack stack,
                                                                EntityPlayer player) {
        return null;
    }

    /**
     * A full replacement for the default HUD's liquid layer, drawn over the glass at nine by
     * nine points and cropped bottom-up for a recharging charge. Put your liquid pixels in
     * the same rows as the built-in mask (rows 1 to 7 of 9), because the recharge crop maps
     * progress onto those rows. The built-in layer is a white mask tinted by
     * {@link #hudLiquidColor}; a custom layer is tinted the same way, except that -1 then
     * means untinted rather than the built-in red. Null keeps the built-in.
     */
    default net.minecraft.util.ResourceLocation hudLiquidTexture(ItemStack stack,
                                                                 EntityPlayer player) {
        return null;
    }
    // Read on the client as well as the server: the journal asks the value methods above for a
    // bare stack of the item, and the viewer it passes may be null while the game is starting.
    // Return a sensible answer for a plain stack and do not require a server there.

    /**
     * An optional language key for this content's journal entry: whatever a player should be
     * told about it beyond the item itself.
     *
     * <p>The entry already shows the item, and its numbers are on its own tooltip, so this is
     * for what nothing else can say. Where the thing is normally found is the usual answer, but
     * it is not required to be: write what is worth writing, or return null and let the entry
     * be the item alone.
     *
     * <p>Return a language key, never finished text, so the entry reads in the player's own
     * language. A pack author can replace or hide it per registry name in this mod's config,
     * because a pack often changes where content comes from; write the truth for your own mod
     * and let them correct it.
     */
    default String journalText(ItemStack stack) {
        return null;
    }
}
