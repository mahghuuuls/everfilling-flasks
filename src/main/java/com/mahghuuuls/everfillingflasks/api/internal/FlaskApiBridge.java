package com.mahghuuuls.everfillingflasks.api.internal;

import com.mahghuuuls.everfillingflasks.api.FlaskModifierSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The seam between the public {@code api} package and the mod's internals. Add-ons must not
 * touch this package: its shape may change between versions without notice, and everything in
 * it is reachable through {@link com.mahghuuuls.everfillingflasks.api.FlaskApi}.
 *
 * <p>Calls arriving before the mod's pre-initialization are buffered and replayed when the
 * implementation binds, so an add-on's registration order against this mod cannot matter.
 */
public abstract class FlaskApiBridge {

    private static FlaskApiBridge instance;
    private static List<FlaskModifierSource> pendingSources = new ArrayList<FlaskModifierSource>();

    public static synchronized void bind(FlaskApiBridge implementation) {
        if (instance != null) {
            org.apache.logging.log4j.LogManager.getLogger("Everfilling Flasks").warn(
                    "Flask API bridge rebound from {} to {}; only the mod itself should bind",
                    instance.getClass().getName(), implementation.getClass().getName());
        }
        instance = implementation;
        if (pendingSources != null) {
            for (FlaskModifierSource source : pendingSources) {
                implementation.registerModifierSourceNow(source);
            }
            pendingSources = null;
        }
    }

    public static synchronized void registerModifierSource(FlaskModifierSource source) {
        if (instance == null) {
            pendingSources.add(source);
        } else {
            instance.registerModifierSourceNow(source);
        }
    }

    protected abstract void registerModifierSourceNow(FlaskModifierSource source);
}
