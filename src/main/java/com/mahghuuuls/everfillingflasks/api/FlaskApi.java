package com.mahghuuuls.everfillingflasks.api;

import com.mahghuuuls.everfillingflasks.api.internal.FlaskApiBridge;

/**
 * The public entry point for other mods: Flask and ingredient registration, modifier sources,
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
     * Registers {@code definition} as the Flask Ingredient behavior of {@code item}, making
     * that item placeable in every Flask's infusion grid. First registration per item wins; a
     * duplicate is refused with a log line, never an exception. The core provides the grid,
     * cost accounting, the over-capacity unusable state, effective-value merging, and
     * post-drink dispatch.
     */
    public static void registerIngredient(net.minecraft.item.Item item,
                                          IngredientDefinition definition) {
        FlaskApiBridge.registerIngredient(item, definition);
    }
}
