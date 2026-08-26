package com.mahghuuuls.everfillingflasks.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * What makes an item a Flask Infusion: its potency cost, and what it does to the hosting
 * Flask while placed in the infusion grid. Register one per {@link net.minecraft.item.Item}
 * through {@link FlaskApi#registerInfusion}, exactly like Flask registration.
 *
 * <p>The core owns everything around the definition: the nine-slot grid, cost accounting, the
 * over-capacity unusable state, merging contributions into effective values, and post-drink
 * dispatch. A definition describes only what its infusion costs and does.
 *
 * <p>Placed infusions are permanent while placed and never consumed by drinking. Their
 * contributions use the same bonus types and the same combination formulas as player Flask
 * modifiers: percentages of one kind, from every source, add together before multiplying the
 * base. Methods are called on the logical server; keep them cheap and free of side effects,
 * except {@link #onDrinkCompleted}.
 */
public interface InfusionDefinition {

    /**
     * Potency this piece costs while placed. Values below 0 are treated as 0. May depend on
     * the piece's NBT; the sum of all placed costs against the Flask's potency decides the
     * over-capacity state.
     */
    int potencyCost(ItemStack infusion);

    /**
     * This piece's contribution to the hosting Flask's effective values, consulted whenever
     * they are computed. An over-capacity grid is inert: while the summed costs exceed the
     * Flask's potency, no placed piece contributes anything. A thrown exception is caught and
     * logged once; the piece then contributes nothing for that computation and everything
     * else applies normally.
     */
    default void contribute(ItemStack infusion, EntityPlayer player, FlaskBonuses bonuses) {
    }

    /**
     * Called on the logical server after a drink of the hosting Flask completes, once per
     * placed piece, after the Flask definition's own
     * {@link FlaskDefinition#onDrinkCompleted}. Isolated the same way: a throw is caught and
     * logged and cannot touch the completed drink.
     */
    default void onDrinkCompleted(ItemStack infusion, ItemStack flask, EntityPlayer player) {
    }
    /**
     * An optional sentence describing what this infusion does, shown on the item tooltip.
     *
     * <p>Supply it when your infusion has something to say that its {@link #contribute}
     * cannot show on its own, such as an effect that happens after a drink. The core reads it
     * for the built-in infusions' own tooltips, so one sentence serves everywhere.
     *
     * <p>Return a translation component rather than finished text, so it reads in the player's
     * own language. Null means "let the journal describe whatever I contribute".
     */
    default net.minecraft.util.text.ITextComponent effectDescription(ItemStack infusion) {
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
