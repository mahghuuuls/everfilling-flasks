package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The one answer to "is this a Flask": an {@link Item} is a Flask exactly when a definition was
 * registered for it. No class check, no item identity check, anywhere.
 *
 * <p>Registrations are refused, never replaced: the first definition for an item keeps it, a
 * duplicate or null registration logs and returns false, and nothing throws. An add-on that
 * loses a race learns it from the log and the return value, and the game keeps loading.
 */
public final class FlaskRegistry {

    private static final Map<Item, FlaskDefinition> DEFINITIONS =
            new ConcurrentHashMap<Item, FlaskDefinition>();

    private FlaskRegistry() {
    }

    /** Registers {@code definition} for {@code item}. False, with a log line, on refusal. */
    public static boolean register(Item item, FlaskDefinition definition) {
        if (item == null || definition == null) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Flask registration refused: item and definition must both be non-null"
                            + " (item={}, definition={})", item, definition);
            return false;
        }
        if (DEFINITIONS.putIfAbsent(item, definition) != null) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Flask registration refused for {}: it already has a definition; the first"
                            + " registration keeps it", item.getRegistryName());
            return false;
        }
        return true;
    }

    public static boolean isFlask(ItemStack stack) {
        return !stack.isEmpty() && DEFINITIONS.containsKey(stack.getItem());
    }

    /** The definition for this stack's item, or null when it is not a Flask. */
    public static FlaskDefinition definition(ItemStack stack) {
        return stack.isEmpty() ? null : DEFINITIONS.get(stack.getItem());
    }
}
