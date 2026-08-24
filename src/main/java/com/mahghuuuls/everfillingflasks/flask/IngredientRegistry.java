package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.IngredientDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The one answer to "is this a Flask Ingredient", plus every walk over a placed grid: cost
 * summing, contribution collecting, and post-drink dispatch. Mirrors {@link FlaskRegistry}:
 * registrations are refused, never replaced, and nothing here throws.
 *
 * <p>Isolation matches the modifier sources: a definition that throws in a walk is logged once
 * per session and skipped for that call; the other pieces apply normally. A grid slot holding
 * an item with no registered definition contributes nothing and costs nothing — it can only
 * exist when a registration disappeared between sessions, and pricing it would mean inventing
 * a number.
 */
public final class IngredientRegistry {

    private static final Map<Item, IngredientDefinition> DEFINITIONS =
            new ConcurrentHashMap<Item, IngredientDefinition>();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private IngredientRegistry() {
    }

    /** Registers {@code definition} for {@code item}. False, with a log line, on refusal. */
    public static boolean register(Item item, IngredientDefinition definition) {
        if (item == null || definition == null) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Ingredient registration refused: item and definition must both be non-null"
                            + " (item={}, definition={})", item, definition);
            return false;
        }
        if (DEFINITIONS.putIfAbsent(item, definition) != null) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Ingredient registration refused for {}: it already has a definition; the"
                            + " first registration keeps it", item.getRegistryName());
            return false;
        }
        return true;
    }

    public static boolean isIngredient(ItemStack stack) {
        return !stack.isEmpty() && DEFINITIONS.containsKey(stack.getItem());
    }

    /** The definition for this stack's item, or null when it is not an ingredient. */
    public static IngredientDefinition definition(ItemStack stack) {
        return stack.isEmpty() ? null : DEFINITIONS.get(stack.getItem());
    }

    /** Test seam, like the modifier registry's: static state must not leak between tests. */
    static void clearForTests() {
        DEFINITIONS.clear();
        FAILED.clear();
    }

    /** Summed potency costs of every placed piece, each floored at 0. */
    public static int usedPotency(NonNullList<ItemStack> grid) {
        int used = 0;
        for (ItemStack piece : grid) {
            IngredientDefinition definition = definition(piece);
            if (definition == null) {
                continue;
            }
            try {
                used += Math.max(0, definition.potencyCost(piece));
            } catch (Throwable failure) {
                logOnce(definition, "potencyCost", failure);
            }
        }
        return used;
    }

    /** Every placed piece's contribution into the one shared accumulator. */
    public static void contribute(NonNullList<ItemStack> grid, EntityPlayer player,
                                  FlaskBonuses bonuses) {
        for (ItemStack piece : grid) {
            IngredientDefinition definition = definition(piece);
            if (definition == null) {
                continue;
            }
            try {
                definition.contribute(piece, player, bonuses);
            } catch (Throwable failure) {
                logOnce(definition, "contribute", failure);
            }
        }
    }

    /** The post-drink hooks, one call per placed piece, each isolated. */
    public static void dispatchDrinkCompleted(NonNullList<ItemStack> grid, ItemStack flask,
                                              EntityPlayer player) {
        for (ItemStack piece : grid) {
            IngredientDefinition definition = definition(piece);
            if (definition == null) {
                continue;
            }
            try {
                definition.onDrinkCompleted(piece, flask, player);
            } catch (Throwable failure) {
                logOnce(definition, "onDrinkCompleted", failure);
            }
        }
    }

    private static void logOnce(IngredientDefinition definition, String method,
                                Throwable failure) {
        if (FAILED.add(definition.getClass().getName() + "#" + method)) {
            EverfillingFlasksMod.LOGGER.error(
                    "Ingredient definition {} failed in {}; that piece is skipped for this call,"
                            + " it stays registered, and this is logged once",
                    definition.getClass().getName(), method, failure);
        }
    }
}
