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
 * except the {@code onDrinkCompleted} hooks.
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
     * they are computed. An inert grid (over capacity, or since 1.2.0 holding a conflicting
     * pair) contributes nothing. A thrown exception is caught and logged once; the piece then
     * contributes nothing for that computation and everything else applies normally.
     *
     * <p>Since 1.2.0 it is also asked on the client, with a null player and a fresh
     * accumulator, to write the tooltip lines when {@link #effectDescription} is null. Answer
     * from the stack; a throw there costs only the generated lines.
     */
    default void contribute(ItemStack infusion, EntityPlayer player, FlaskBonuses bonuses) {
    }

    /**
     * Called on the logical server after a drink of the hosting Flask completes, once per
     * placed piece, after the Flask definition's own
     * {@link FlaskDefinition#onDrinkCompleted(ItemStack, EntityPlayer, DrinkOutcome)}.
     * Isolated the same way: a throw is caught and logged and cannot touch the completed
     * drink.
     */
    default void onDrinkCompleted(ItemStack infusion, ItemStack flask, EntityPlayer player) {
    }

    /**
     * Whether this infusion refuses to share a grid with {@code other}. Two pieces conflict
     * when either side's definition says so, so a one-sided answer is enough. Whether two
     * identical items conflict is this method's own answer, with {@code other} an equal item.
     *
     * <p>A conflicting piece cannot be placed: click, swap, and shift-click all refuse it, on
     * both sides, and the refused piece stays where it was. A grid that holds a conflict anyway
     * (an old save, a command, a creative copy, a definition that changed) is inert exactly like
     * an over-capacity grid: no piece contributes, no infusion hook runs, and the Flask cannot
     * start a drink until a piece is removed. Called on both sides with no player; answer from
     * the two stacks alone. A throw is logged once and counts as no conflict. Since 1.2.0.
     */
    default boolean conflictsWith(ItemStack self, ItemStack other) {
        return false;
    }

    /**
     * The same moment as {@link #onDrinkCompleted(ItemStack, ItemStack, EntityPlayer)}, with the
     * drink's outcome: the very object the hosting Flask's own hook received, so its
     * {@link DrinkOutcome#effectPower()} already includes every placed piece's contribution. The
     * core calls this form only; its default forwards to the three-argument form, so a
     * definition written against 1.1.0 runs exactly once per placed piece, as before. Override
     * one or the other, not both. Since 1.2.0.
     */
    default void onDrinkCompleted(ItemStack infusion, ItemStack flask, EntityPlayer player,
                                  DrinkOutcome outcome) {
        onDrinkCompleted(infusion, flask, player);
    }

    /**
     * An optional sentence describing what this infusion does, shown on the item tooltip.
     *
     * <p>Supply it when your infusion has something to say that its {@link #contribute}
     * cannot show on its own, such as an effect that happens after a drink. Since 1.2.0 the core
     * writes every registered infusion's tooltip, directly under the item name: a header, the
     * potency cost, then this sentence; or, when this returns null, one signed line per bonus
     * channel {@link #contribute} changes ("+20% healing", "-15% drink speed"). Your item's own
     * tooltip lines come after those, so write only extras there.
     *
     * <p>Return a translation component rather than finished text, so it reads in the player's
     * own language.
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
