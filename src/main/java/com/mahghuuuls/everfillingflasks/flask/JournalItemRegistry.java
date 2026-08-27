package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import net.minecraft.item.Item;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Items that belong in the journal without being a Flask or an infusion.
 *
 * <p>The Everlasting Seed is the first: required, uncraftable, and explained nowhere else. A
 * later add-on will have its own — a vessel part, a brewing tool, whatever it invents — and this
 * is where those go, so an add-on never has to build a book of its own for one item.
 *
 * <p>Registration is presentation only. Nothing here changes what an item does; an item that
 * also behaves as an infusion registers that separately and appears in both places.
 */
public final class JournalItemRegistry {

    /** Item to the language key of its entry text, or an empty string for a page with none. */
    private static final Map<Item, String> ENTRIES = new ConcurrentHashMap<Item, String>();

    private JournalItemRegistry() {
    }

    /** First registration per item wins, like every other registry here. Never throws. */
    public static boolean register(Item item, String textKey) {
        if (item == null) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Journal item registration refused: the item must not be null");
            return false;
        }
        String previous = ENTRIES.putIfAbsent(item, textKey == null ? "" : textKey);
        if (previous != null) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Journal item registration refused for {}: it already has an entry; the "
                            + "first registration keeps it", item.getRegistryName());
            return false;
        }
        return true;
    }

    /** Every registration, read-only, for the journal to walk. */
    public static Map<Item, String> all() {
        return Collections.unmodifiableMap(ENTRIES);
    }

    /** Test seam: static state must not leak between tests. */
    static void clearForTests() {
        ENTRIES.clear();
    }
}
