package com.mahghuuuls.everfillingflasks.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * What makes an item a Flask Ingredient: its potency cost, and what it does to the hosting
 * Flask while placed in the infusion grid. Register one per {@link net.minecraft.item.Item}
 * through {@link FlaskApi#registerIngredient}, exactly like Flask registration.
 *
 * <p>The core owns everything around the definition: the nine-slot grid, cost accounting, the
 * over-capacity unusable state, merging contributions into effective values, and post-drink
 * dispatch. A definition describes only what its ingredient costs and does.
 *
 * <p>Placed ingredients are permanent while placed and never consumed by drinking. Their
 * contributions use the same bonus types and the same combination formulas as player Flask
 * modifiers: percentages of one kind, from every source, add together before multiplying the
 * base. Methods are called on the logical server; keep them cheap and free of side effects,
 * except {@link #onDrinkCompleted}.
 */
public interface IngredientDefinition {

    /**
     * Potency this piece costs while placed. Values below 0 are treated as 0. May depend on
     * the piece's NBT; the sum of all placed costs against the Flask's potency decides the
     * over-capacity state.
     */
    int potencyCost(ItemStack ingredient);

    /**
     * This piece's contribution to the hosting Flask's effective values, consulted whenever
     * they are computed. An over-capacity grid is inert: while the summed costs exceed the
     * Flask's potency, no placed piece contributes anything. A thrown exception is caught and
     * logged once; the piece then contributes nothing for that computation and everything
     * else applies normally.
     */
    default void contribute(ItemStack ingredient, EntityPlayer player, FlaskBonuses bonuses) {
    }

    /**
     * Called on the logical server after a drink of the hosting Flask completes, once per
     * placed piece, after the Flask definition's own
     * {@link FlaskDefinition#onDrinkCompleted}. Isolated the same way: a throw is caught and
     * logged and cannot touch the completed drink.
     */
    default void onDrinkCompleted(ItemStack ingredient, ItemStack flask, EntityPlayer player) {
    }
    // Read on the client as well as the server: the journal asks the value methods above for a
    // bare stack of the item, and the viewer it passes may be null while the game is starting.
    // Return a sensible answer for a plain stack and do not require a server there.

    /**
     * An optional language key for a paragraph about this content, shown at the top of its
     * journal entry. Null means the entry shows only what the mod can work out on its own,
     * which is a complete entry already; this is enrichment, never a requirement (REQ-042 of
     * the journal requirements).
     */
    default String journalDescription(ItemStack stack) {
        return null;
    }

    /**
     * An optional language key answering "where is this normally obtained", shown under its own
     * heading. Separate from any crafting recipe on purpose: a recipe says how it can be made,
     * this says where it usually comes from, and content can have either, both, or neither. A
     * pack author can replace or hide it in the config, so return the truth for your own mod and
     * let packs correct it (REQ-041).
     */
    default String journalHint(ItemStack stack) {
        return null;
    }
}
