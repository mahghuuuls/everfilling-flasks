package com.mahghuuuls.everfillingflasks.client.journal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.journal.JournalTextOverrides;
import java.util.function.Supplier;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Composes one journal entry from one registration.
 *
 * <p>An entry shows the thing itself, whatever is worth saying about it, and how it is made. It
 * deliberately does not restate the numbers: those live on the item's own tooltip, and the item
 * drawn at the top of the entry is hoverable, so the tooltip is one hover away from the page
 * (owner decision 2026-08-26, after seeing the same values in both places).
 *
 * <p>Every word here comes from the language file, and the name is localized before it is
 * stored, which is what lets the list read in the player's own language (REQ-042).
 */
@SideOnly(Side.CLIENT)
final class JournalEntryWriter {

    /** A single space: a title the book treats as given, and therefore draws as nothing. */
    private static final String BLANK_TITLE = " ";

    /** The page is 116 points wide; a title needs a margin on each side of it. */
    private static final int TITLE_WIDTH = 108;

    private JournalEntryWriter() {}

    /** A Flask entry: the Flask itself, what is worth saying, and its recipe if it has one. */
    static JsonObject flask(Item item, FlaskDefinition definition, String categoryId,
            String recipeName, int sortRank) {
        ItemStack stack = new ItemStack(item);
        return entry(stack, categoryId, sortRank,
                text(item, () -> definition.journalText(stack)), recipeName);
    }

    /** An infusion entry, built the same way. */
    static JsonObject infusion(Item item, InfusionDefinition definition, String categoryId,
            String recipeName, int sortRank) {
        ItemStack stack = new ItemStack(item);
        return entry(stack, categoryId, sortRank,
                text(item, () -> definition.journalText(stack)), recipeName);
    }

    /**
     * What the entry says, or nothing at all.
     *
     * <p>The pack author is asked first, because only they know what their pack did with this
     * content: their text replaces the mod's, and their empty text hides it. Asking them first
     * also means a mod whose own text is broken can still be corrected, which is the case that
     * most needs correcting. With no override the content's own text stands, and with neither
     * the entry is the item alone (REQ-041).
     */
    private static String text(Item item, Supplier<String> ownKey) {
        ResourceLocation name = item.getRegistryName();
        JournalTextOverrides overrides = ConfigSnapshot.current().textOverrides();
        if (name != null && overrides.has(name.toString())) {
            return overrides.text(name.toString());
        }
        String key = ownKey.get();
        return key == null || key.isEmpty() ? "" : I18n.format(key);
    }

    /**
     * The entry description.
     *
     * <p>The alphabetical position is handed in rather than left to the book, so the order
     * cannot depend on how a name happens to be written.
     */
    private static JsonObject entry(ItemStack stack, String categoryId, int sortRank, String body,
            String recipeName) {
        JsonArray pages = new JsonArray();
        pages.add(spotlightPage(stack, body));
        if (recipeName != null) {
            pages.add(craftingPage(recipeName));
        }

        JsonObject json = new JsonObject();
        json.addProperty("name", fittedName(stack));
        json.addProperty("category", categoryId);
        json.addProperty("icon", String.valueOf(stack.getItem().getRegistryName()));
        json.addProperty("read_by_default", true);
        json.addProperty("sortnum", sortRank);
        json.add("pages", pages);
        return json;
    }

    /**
     * The item itself at the top of the page, with whatever the entry has to say beneath it.
     * The drawn item keeps its tooltip, so its numbers are always within reach.
     */
    private static JsonObject spotlightPage(ItemStack stack, String body) {
        JsonObject page = new JsonObject();
        page.addProperty("type", "spotlight");
        page.addProperty("item", String.valueOf(stack.getItem().getRegistryName()));
        // The entry's own heading already names it; the page must not say it twice.
        page.addProperty("title", BLANK_TITLE);
        page.addProperty("text", body);
        return page;
    }

    private static JsonObject craftingPage(String recipeName) {
        JsonObject page = new JsonObject();
        page.addProperty("type", CenteredCraftingPage.TYPE);
        page.addProperty("recipe", recipeName);
        return page;
    }

    /**
     * The entry name, shortened if it cannot fit the page.
     *
     * <p>The book draws an entry's title itself, on one line, and does not wrap: a long name
     * simply runs off the page. Rather than let that happen, a name too wide is cut and ends in
     * an ellipsis. Ordering is unaffected, because the alphabetical position is worked out from
     * the full name and handed to the book separately.
     */
    private static String fittedName(ItemStack stack) {
        String full = plainName(stack);
        net.minecraft.client.gui.FontRenderer font =
                net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
        if (font == null || font.getStringWidth(full) <= TITLE_WIDTH) {
            return full;
        }
        String ellipsis = "...";
        String kept = font.trimStringToWidth(full, TITLE_WIDTH - font.getStringWidth(ellipsis));
        return kept + ellipsis;
    }

    /** The name as the player reads it, which is what the order follows. */
    static String plainName(ItemStack stack) {
        return stack.getDisplayName();
    }
}
