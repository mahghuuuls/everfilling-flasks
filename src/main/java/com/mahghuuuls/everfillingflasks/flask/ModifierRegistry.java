package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.FlaskModifierSource;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The registered player Flask modifier sources. {@link #collect} asks every source to fill one
 * accumulator. A source that throws is retried on every collect but logged only once per
 * session; whatever it wrote before throwing stays in the accumulator, and the other sources
 * apply normally, so one broken add-on can degrade only its own contribution.
 */
public final class ModifierRegistry {

    private static final List<FlaskModifierSource> SOURCES =
            new CopyOnWriteArrayList<FlaskModifierSource>();
    private static final Set<String> FAILED_SOURCES =
            ConcurrentHashMap.newKeySet();

    private ModifierRegistry() {
    }

    public static void register(FlaskModifierSource source) {
        if (source == null) {
            EverfillingFlasksMod.LOGGER.warn("Ignored a null flask modifier source registration");
            return;
        }
        SOURCES.add(source);
    }

    /** Test seam: the registry is static and tests must not leak sources into each other. */
    static void clearForTests() {
        SOURCES.clear();
        FAILED_SOURCES.clear();
    }

    public static FlaskBonuses collect(EntityPlayer player) {
        FlaskBonuses bonuses = new FlaskBonuses();
        for (FlaskModifierSource source : SOURCES) {
            try {
                source.contribute(player, bonuses);
            } catch (Throwable failure) {
                if (FAILED_SOURCES.add(source.getClass().getName())) {
                    EverfillingFlasksMod.LOGGER.error(
                            "Flask modifier source {} failed; its bonuses are dropped for this"
                                    + " computation, it stays registered, and this is logged once",
                            source.getClass().getName(), failure);
                }
            }
        }
        return bonuses;
    }
}
