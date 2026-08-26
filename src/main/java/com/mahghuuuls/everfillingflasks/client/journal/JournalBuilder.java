package com.mahghuuuls.everfillingflasks.client.journal;

import com.google.gson.JsonObject;
import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.Tags;
import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.InfusionRegistry;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.patchouli.api.BookContentsReloadEvent;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.ClientBookRegistry;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

/**
 * Fills the journal from this mod's own registries every time Patchouli finishes loading the
 * book (ARC-011).
 *
 * <p>Nothing about the journal is stored on disk beyond the book itself: the two sections and,
 * from IMP-016, every entry are composed here and handed to Patchouli's own parser. That is what
 * lets an add-on appear in the journal by registering a Flask and nothing else (REQ-037), and it
 * is why the text is localized here rather than duplicated per language in asset files (REQ-042).
 *
 * <p>Patchouli raises a reload event after it has cleared and reloaded a book, but that event
 * cannot say which book it is about: it carries the book's texture path, which every book using
 * the default texture shares. So the trigger here is the journal's own emptiness rather than the
 * event's identity, which also covers the reloads that reach us by no event at all.
 *
 * <p>The numbers shown come from the client's own configuration file. In singleplayer, and in
 * the normal pack case where the server ships the same file to its players, that is the truth. A
 * server that changes its configuration without giving players the same file will show them
 * stale numbers here; the HUD and the Flask screen stay correct either way, because those read
 * the server's own state messages. Recorded as a 1.0 limitation.
 *
 * <p>Every failure is contained: a fault costs the journal, never a Flask (REQ-043).
 */
@SideOnly(Side.CLIENT)
public final class JournalBuilder {

    /** Section ids. Player-visible names come from the language file. */
    private static final String FLASKS = "flasks";
    private static final String INFUSIONS = "infusions";

    /** Sections are shown in this order rather than alphabetically; entries inside sort A to Z. */
    private static final int FLASKS_SORT = 0;
    private static final int INFUSIONS_SORT = 1;

    private static boolean buildFailed;

    /** One log line per broken registration per session, not one per reload. */
    private static final Set<ResourceLocation> failedEntries = new HashSet<>();

    /** Registry names the journal actually described this rebuild, for the override check. */
    private final Set<String> described = new HashSet<>();

    /** One log line per unmatched override name per session. */
    private static final Set<String> reportedOverrides = new HashSet<>();

    /** One log line per recipe that failed the scan, per session. */
    private static final Set<String> recipeScanFailed = new HashSet<>();

    /**
     * How many registrations the journal was last built from. Content may be registered at any
     * time, including after the book was first filled, so a count that no longer matches means
     * the journal is out of date and is built again.
     */
    private static int builtFrom = -1;

    /**
     * A book somewhere finished reloading.
     *
     * <p>The event cannot say which one: it carries the book's texture path, and every book that
     * does not override the texture shares the default. So this asks the only question that can
     * be answered, which is whether our own journal still holds its sections, and rebuilds when
     * it does not. Patchouli clears a book's categories and entries as it reloads, so a wiped
     * journal is exactly the state that needs rebuilding, whichever book's event delivered the
     * news. A rebuild is idempotent, so an extra one costs nothing.
     */
    @SubscribeEvent
    public void onBookContentsReloaded(BookContentsReloadEvent event) {
        ensureCurrent();
    }

    /**
     * Rebuilds the journal if it is missing or out of date.
     *
     * <p>Any resource reload (a language change, a resource pack, F3+T) makes Patchouli clear
     * the book, and this puts it back. A mod registering a Flask or an infusion later than the
     * first build changes the count, and this picks that up too.
     */
    static void ensureCurrent() {
        try {
            Book book = BookRegistry.INSTANCE.books.get(JournalBridge.BOOK);
            if (book == null || book.contents == null) {
                return;
            }
            boolean sectionsGone = !book.contents.categories.containsKey(sectionKey(FLASKS))
                    || !book.contents.categories.containsKey(sectionKey(INFUSIONS));
            if (sectionsGone || registrationCount() != builtFrom) {
                build();
            }
        } catch (Throwable failure) {
            report(failure);
        }
    }

    private static int registrationCount() {
        return FlaskRegistry.all().size() + InfusionRegistry.all().size();
    }

    private static void build() {
        try {
            Book book = BookRegistry.INSTANCE.books.get(JournalBridge.BOOK);
            if (book == null || book.contents == null) {
                return;
            }
            new JournalBuilder().rebuild(book, book.contents);
        } catch (Throwable failure) {
            report(failure);
        }
    }

    private static void report(Throwable failure) {
        if (!buildFailed) {
            buildFailed = true;
            EverfillingFlasksMod.LOGGER.warn(
                    "The journal could not be built; the rest of the mod is unaffected.", failure);
        }
    }

    private void rebuild(Book book, BookContents contents) {
        addSection(book, contents, FLASKS, FLASKS_SORT, "everfillingflasks:rare_flask");
        addSection(book, contents, INFUSIONS, INFUSIONS_SORT,
                "everfillingflasks:sunpetal_leaf");

        // One pass over the recipe registry for the whole rebuild, rather than one search per
        // entry. A recipe a pack or the config disabled is simply not registered, so it is
        // absent here too, which is exactly what REQ-040 asks for.
        Map<Item, String> recipes = recipesByOutput();
        builtFrom = registrationCount();

        Map<Item, Integer> flaskRanks = alphabeticalRanks(FlaskRegistry.all().keySet());
        for (Map.Entry<Item, FlaskDefinition> flask : FlaskRegistry.all().entrySet()) {
            Item item = flask.getKey();
            addEntry(book, contents, item, "flask", () -> JournalEntryWriter.flask(
                    item, flask.getValue(), sectionId(FLASKS), recipes.get(item),
                    flaskRanks.get(item)));
        }
        Map<Item, Integer> infusionRanks = alphabeticalRanks(InfusionRegistry.all().keySet());
        for (Map.Entry<Item, InfusionDefinition> infusion
                : InfusionRegistry.all().entrySet()) {
            Item item = infusion.getKey();
            addEntry(book, contents, item, "infusion", () -> JournalEntryWriter.infusion(
                    item, infusion.getValue(), sectionId(INFUSIONS),
                    recipes.get(item), infusionRanks.get(item)));
        }
        dropStaleRecipeLinks(contents);
        reportUnmatchedOverrides();
    }

    /**
     * Patchouli remembers which entry explains which item, so that looking up an item can jump
     * to its page. A rebuild replaces our entry objects, and Patchouli clears that memory only
     * during its own reload, so the replaced objects would stay pointed at from there and the
     * jump would land on an entry no longer in the book. Dropped here, in the one place that
     * knows a rebuild happened.
     */
    private void dropStaleRecipeLinks(BookContents contents) {
        contents.recipeMappings.values().removeIf(
                mapping -> mapping == null || !contents.entries.containsValue(mapping.getLeft()));
    }

    /**
     * Each item's place in A-to-Z order by the name the player reads, with no colour codes in
     * the way.
     *
     * <p>The book orders entries by a number first and their raw name second. The names carry
     * rarity colours, and a colour code at the front of a name would otherwise decide the order
     * before the first letter did, so the order is worked out here from the plain names and
     * handed to the book as that number.
     */
    private Map<Item, Integer> alphabeticalRanks(Set<Item> items) {
        List<Item> sorted = new ArrayList<>(items);
        Collections.sort(sorted, new Comparator<Item>() {
            @Override
            public int compare(Item left, Item right) {
                int byName = JournalEntryWriter.plainName(new ItemStack(left))
                        .compareToIgnoreCase(JournalEntryWriter.plainName(new ItemStack(right)));
                // Two items sharing a display name still need one settled order, or the book
                // would show them in whichever order the registry happened to hand over.
                return byName != 0 ? byName
                        : String.valueOf(left.getRegistryName())
                                .compareTo(String.valueOf(right.getRegistryName()));
            }
        });
        Map<Item, Integer> ranks = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            ranks.put(sorted.get(i), i);
        }
        return ranks;
    }

    /**
     * A text override naming something no mod registered is silent otherwise, and a pack author
     * would have no way to find their typo. Said once per name per session (REQ-041).
     */
    private void reportUnmatchedOverrides() {
        for (String name : ConfigSnapshot.current().textOverrides().names()) {
            if (described.contains(name) || !reportedOverrides.add(name)) {
                continue;
            }
            EverfillingFlasksMod.LOGGER.warn(
                    "journal.textOverrides names {}, which is not a registered Flask or "
                            + "infusion; that line does nothing.", name);
        }
    }

    /**
     * Adds one entry, guarded on its own: a definition that throws costs its own entry and
     * nothing else, and says so once per session (REQ-043).
     */
    private void addEntry(Book book, BookContents contents, Item item, String prefix,
            Supplier<JsonObject> composer) {
        ResourceLocation itemName = item.getRegistryName();
        if (itemName == null) {
            return;
        }
        ResourceLocation key = new ResourceLocation(Tags.MOD_ID,
                prefix + "_" + itemName.getNamespace() + "_" + itemName.getPath());
        try {
            BookEntry entry =
                    ClientBookRegistry.INSTANCE.gson.fromJson(composer.get(), BookEntry.class);
            entry.setBook(book);
            BookCategory section = entry.getCategory();
            if (section == null) {
                return;
            }
            // Built first, published second. Patchouli turns any page failure into a throw, and
            // a half-built entry left in a section renders as a broken page inside Patchouli's
            // own drawing code, where no guard of ours can reach it.
            entry.build(key);
            section.addEntry(entry);
            contents.entries.put(key, entry);
            described.add(itemName.toString());
        } catch (Throwable failure) {
            if (failedEntries.add(itemName)) {
                EverfillingFlasksMod.LOGGER.warn(
                        "No journal entry could be built for {}; the rest of the journal is "
                                + "unaffected.", itemName, failure);
            }
        }
    }

    /**
     * Registry name of a crafting recipe per output item.
     *
     * <p>Every recipe in the game is asked its output, and any recipe may come from any mod, so
     * each one is asked inside its own guard: a recipe that throws or answers with nothing costs
     * itself alone, never the journal (REQ-043). Recipes from the item's own mod are preferred,
     * because another mod's conversion or repair recipe for the same item would otherwise be
     * shown as the way to make it (REQ-040).
     */
    private Map<Item, String> recipesByOutput() {
        Map<Item, String> recipes = new HashMap<>();
        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            try {
                collectRecipe(recipe, recipes);
            } catch (Throwable failure) {
                if (recipeScanFailed.add(String.valueOf(recipe.getRegistryName()))) {
                    EverfillingFlasksMod.LOGGER.warn(
                            "The journal skipped the recipe {}, which failed when asked for its "
                                    + "output; every other entry is unaffected.",
                            recipe.getRegistryName(), failure);
                }
            }
        }
        return recipes;
    }

    private void collectRecipe(IRecipe recipe, Map<Item, String> recipes) {
        ItemStack output = recipe.getRecipeOutput();
        ResourceLocation name = recipe.getRegistryName();
        if (output == null || output.isEmpty() || name == null) {
            return;
        }
        Item item = output.getItem();
        ResourceLocation itemName = item.getRegistryName();
        boolean ownRecipe = itemName != null && itemName.getNamespace().equals(name.getNamespace());
        String held = recipes.get(item);
        // The item's own mod wins; otherwise the first one found stands.
        if (held == null || (ownRecipe && !heldIsOwn(held, itemName))) {
            recipes.put(item, name.toString());
        }
    }

    private boolean heldIsOwn(String heldRecipeName, ResourceLocation itemName) {
        return itemName != null
                && heldRecipeName.startsWith(itemName.getNamespace() + ":");
    }

    private static String sectionId(String id) {
        return Tags.MOD_ID + ":" + id;
    }

    private static ResourceLocation sectionKey(String id) {
        return new ResourceLocation(Tags.MOD_ID, id);
    }

    /**
     * One section, composed as Patchouli's own category description and parsed by Patchouli's own
     * parser, because the fields of its category type are not ours to set directly.
     */
    private void addSection(Book book, BookContents contents, String id, int sort, String icon) {
        JsonObject json = new JsonObject();
        json.addProperty("name", I18n.format("everfillingflasks.journal.section." + id));
        json.addProperty("description",
                I18n.format("everfillingflasks.journal.section." + id + ".description"));
        json.addProperty("icon", icon);
        json.addProperty("sortnum", sort);

        BookCategory section =
                ClientBookRegistry.INSTANCE.gson.fromJson(json, BookCategory.class);
        section.setBook(book);
        ResourceLocation key = new ResourceLocation(Tags.MOD_ID, id);
        contents.categories.put(key, section);
        section.build(key);
    }
}
