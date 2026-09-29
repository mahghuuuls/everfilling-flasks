package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.api.DrinkOutcome;
import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The one answer to "is this a Flask Infusion", plus every walk over a placed grid: cost
 * summing, contribution collecting, and post-drink dispatch. Mirrors {@link FlaskRegistry}:
 * registrations are refused, never replaced, and nothing here throws.
 *
 * <p>Isolation matches the modifier sources: a definition that throws in a walk is logged once
 * per session and skipped for that call; the other pieces apply normally. A grid slot holding
 * an item with no registered definition contributes nothing and costs nothing — it can only
 * exist when a registration disappeared between sessions, and pricing it would mean inventing
 * a number.
 */
public final class InfusionRegistry {

    private static final Map<Item, InfusionDefinition> DEFINITIONS =
            new ConcurrentHashMap<Item, InfusionDefinition>();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private InfusionRegistry() {
    }

    /** Registers {@code definition} for {@code item}. False, with a log line, on refusal. */
    public static boolean register(Item item, InfusionDefinition definition) {
        if (item == null || definition == null) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Infusion registration refused: item and definition must both be non-null"
                            + " (item={}, definition={})", item, definition);
            return false;
        }
        if (DEFINITIONS.putIfAbsent(item, definition) != null) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Infusion registration refused for {}: it already has a definition; the"
                            + " first registration keeps it", item.getRegistryName());
            return false;
        }
        return true;
    }

    public static boolean isInfusion(ItemStack stack) {
        return !stack.isEmpty() && DEFINITIONS.containsKey(stack.getItem());
    }

    /** The definition for this stack's item, or null when it is not an infusion. */
    public static InfusionDefinition definition(ItemStack stack) {
        return stack.isEmpty() ? null : DEFINITIONS.get(stack.getItem());
    }

    /**
     * Every registration, read-only, for code that presents the whole catalogue rather than
     * answering about one stack. The journal is the only caller; gameplay always asks per stack.
     */
    public static java.util.Map<Item, InfusionDefinition> all() {
        return java.util.Collections.unmodifiableMap(DEFINITIONS);
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
            InfusionDefinition definition = definition(piece);
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

    /**
     * Whether two pieces refuse to share a grid (REQ-051): either side's definition saying so
     * is enough. A throw is logged once per definition and counts as no conflict, so a broken
     * add-on cannot lock a player's grid. Pure over the stacks and the registry, so the client
     * and the server always agree.
     */
    public static boolean conflicts(ItemStack a, ItemStack b) {
        return declares(a, b) || declares(b, a);
    }

    private static boolean declares(ItemStack self, ItemStack other) {
        InfusionDefinition definition = definition(self);
        if (definition == null || other.isEmpty()) {
            return false;
        }
        try {
            return definition.conflictsWith(self, other);
        } catch (Throwable failure) {
            logOnce(definition, "conflictsWith", failure);
            return false;
        }
    }

    /** The first placed piece {@code candidate} conflicts with, or empty when none does. */
    public static ItemStack firstConflict(NonNullList<ItemStack> grid, ItemStack candidate) {
        for (ItemStack piece : grid) {
            if (!piece.isEmpty() && conflicts(candidate, piece)) {
                return piece;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Whether any two placed pieces conflict: the grid is then inert. */
    public static boolean hasConflict(NonNullList<ItemStack> grid) {
        for (int i = 0; i < grid.size(); i++) {
            ItemStack a = grid.get(i);
            if (a.isEmpty()) {
                continue;
            }
            for (int j = i + 1; j < grid.size(); j++) {
                ItemStack b = grid.get(j);
                if (!b.isEmpty() && conflicts(a, b)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Every placed piece's contribution into the one shared accumulator. */
    public static void contribute(NonNullList<ItemStack> grid, EntityPlayer player,
                                  FlaskBonuses bonuses) {
        for (ItemStack piece : grid) {
            InfusionDefinition definition = definition(piece);
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

    /**
     * The post-drink hooks, one call per placed piece, each isolated, each through the outcome
     * form so every piece sees the same outcome the Flask's own hook saw.
     */
    public static void dispatchDrinkCompleted(NonNullList<ItemStack> grid, ItemStack flask,
                                              EntityPlayer player, DrinkOutcome outcome) {
        for (ItemStack piece : grid) {
            InfusionDefinition definition = definition(piece);
            if (definition == null) {
                continue;
            }
            try {
                definition.onDrinkCompleted(piece, flask, player, outcome);
            } catch (Throwable failure) {
                logOnce(definition, "onDrinkCompleted", failure);
            }
        }
    }

    private static void logOnce(InfusionDefinition definition, String method,
                                Throwable failure) {
        if (FAILED.add(definition.getClass().getName() + "#" + method)) {
            EverfillingFlasksMod.LOGGER.error(
                    "Infusion definition {} failed in {}; that piece is skipped for this call,"
                            + " it stays registered, and this is logged once",
                    definition.getClass().getName(), method, failure);
        }
    }
}
