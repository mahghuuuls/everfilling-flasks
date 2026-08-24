package com.mahghuuuls.everfillingflasks.api;

import com.mahghuuuls.everfillingflasks.api.internal.FlaskApiBridge;

/**
 * The public entry point for other mods. Grows with the framework; today it accepts player
 * Flask modifier sources. Flask registration, queries, and the read-only snapshot join as the
 * remaining framework slices land.
 *
 * <p>Safe on both sides and from any loading phase: calls made before this mod initializes are
 * buffered and applied when it does.
 */
public final class FlaskApi {

    private FlaskApi() {
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
}
