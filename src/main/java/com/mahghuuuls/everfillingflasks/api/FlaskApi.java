package com.mahghuuuls.everfillingflasks.api;

import com.mahghuuuls.everfillingflasks.api.internal.FlaskApiBridge;

/**
 * The public entry point for other mods: Flask and infusion registration, modifier sources,
 * the Flask queries, and the read-only state snapshot.
 *
 * <p>Safe on both sides and from any loading phase: registrations made before this mod
 * initializes are buffered and applied when it does, and queries answer safely (no Flasks, an
 * empty snapshot) rather than throwing. Registration works from your mod's pre-initialization
 * or later; the usual place is your init, one phase after this mod binds.
 */
public final class FlaskApi {

    private FlaskApi() {
    }

    /**
     * Registers {@code definition} as the Flask behavior of {@code item}, making that item a
     * Flask: equippable in the Flask slot, drinkable, recharged, and carrying an infusion
     * grid. First registration per item wins; a duplicate is refused with a log line, never an
     * exception. Your item stays yours — the core never casts it, only looks it up.
     */
    public static void registerFlask(net.minecraft.item.Item item, FlaskDefinition definition) {
        FlaskApiBridge.registerFlask(item, definition);
    }

    /** Whether this stack is a registered Flask. False for empty stacks and before binding. */
    public static boolean isFlask(net.minecraft.item.ItemStack stack) {
        return FlaskApiBridge.isFlask(stack);
    }

    /** The definition behind a Flask stack, or null when it is not one. */
    public static FlaskDefinition definition(net.minecraft.item.ItemStack stack) {
        return FlaskApiBridge.definition(stack);
    }

    /**
     * A read-only snapshot of this player's Flask state. On the logical server: authoritative,
     * for any player. On the client: the synced mirror, for the local player only — any other
     * player answers empty, because a client never knows another player's Flask state.
     */
    public static FlaskSnapshot snapshot(net.minecraft.entity.player.EntityPlayer player) {
        return FlaskApiBridge.snapshot(player);
    }

    /**
     * Registers a source of player Flask modifiers. Sources are consulted when effective Flask
     * values are computed: at drink start, on charge changes, on state sync, and about once per
     * second otherwise, never every tick, so implementations should still be cheap. A source
     * that throws keeps being consulted but is logged only once; the others apply normally.
     */
    public static void registerModifierSource(FlaskModifierSource source) {
        FlaskApiBridge.registerModifierSource(source);
    }

    /**
     * Registers {@code definition} as the Flask Infusion behavior of {@code item}, making
     * that item placeable in every Flask's infusion grid. First registration per item wins; a
     * duplicate is refused with a log line, never an exception. The core provides the grid,
     * cost accounting, the over-capacity unusable state, effective-value merging, and
     * post-drink dispatch.
     */
    public static void registerInfusion(net.minecraft.item.Item item,
                                          InfusionDefinition definition) {
        FlaskApiBridge.registerInfusion(item, definition);
    }

    /**
     * Gives an item a page in the journal's Items section, for something that belongs to the
     * Flask ecosystem without being a Flask or an infusion.
     *
     * <p>Presentation only: nothing about the item changes. The page shows the item and, when a
     * language key is given, whatever that key says about it; pass null for a page that is just
     * the item. A pack author can replace or hide that text per registry name, the same way they
     * can for any other entry.
     */
    public static void registerJournalItem(net.minecraft.item.Item item, String textKey) {
        FlaskApiBridge.registerJournalItem(item, textKey);
    }

    /**
     * Pushes the equipped Flask's current refill forward by up to {@code ticks}, for a Flask
     * that refills faster on events the core does not know about. Charges complete as they
     * would under the regular tick, the remainder carries into the next charge, and the Flask
     * never exceeds its maximum. Returns the ticks actually applied.
     *
     * <p>Logical server only, and on the server thread: call it from a tick or event handler,
     * not from a network handler's own thread. On the client, or before this mod binds, nothing
     * changes and 0 is returned. With no equipped Flask, a full Flask, or {@code ticks <= 0}, likewise 0. The
     * push applies even while the Inhibited effect is pausing the refill: the pause stops the
     * clock, and this is not the clock. The change is synced like any charge change. Since 1.1.0.
     */
    public static int advanceRecharge(net.minecraft.entity.player.EntityPlayer player, int ticks) {
        return FlaskApiBridge.advanceRecharge(player, ticks);
    }
}
