package com.mahghuuuls.everfillingflasks.client.journal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.api.IngredientDefinition;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.flask.EffectiveFlask;
import com.mahghuuuls.everfillingflasks.flask.FlaskMechanics;
import com.mahghuuuls.everfillingflasks.journal.JournalHintOverrides;
import java.util.function.Supplier;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Composes one journal entry from one registration.
 *
 * <p>Every number here is asked of the definition that owns it, so the journal cannot drift from
 * the behaviour (REQ-038, REQ-039). Every word here comes from the language file, so the entry is
 * localized before it is stored, which is also what makes the alphabetical order follow the names
 * the player reads (REQ-042, REQ-022 of the owner's document).
 */
@SideOnly(Side.CLIENT)
final class JournalEntryWriter {

    /** Patchouli's line break inside a text page. */
    private static final String BREAK = "$(br)";
    private static final String PARAGRAPH = "$(br2)";

    private static final int TICKS_PER_SECOND = 20;

    private JournalEntryWriter() {}

    /** A Flask entry: its own values, its recipe when one is registered, its source mod. */
    static JsonObject flask(Item item, FlaskDefinition definition, EntityPlayer viewer,
            String categoryId, String recipeName) {
        ItemStack stack = new ItemStack(item);
        StringBuilder text = new StringBuilder();
        describe(text, definition.journalDescription(stack));
        // The definition's own numbers, put through the same function the game puts them
        // through, with no bonuses. That function owns the floors (a drink can never be shorter
        // than its minimum, a Flask never holds fewer than one charge), so a configured value
        // under a floor is displayed as what the player will actually get rather than as what
        // the file says.
        EffectiveFlask shown = FlaskMechanics.effective(
                definition.maxCharges(stack, viewer),
                definition.healPercentage(stack, viewer),
                definition.rechargeTicks(stack, viewer),
                definition.drinkTicks(stack, viewer),
                definition.hitThreshold(stack, viewer),
                new FlaskBonuses());
        line(text, I18n.format("everfillingflasks.journal.flask.charges", shown.maxCharges()));
        line(text, I18n.format("everfillingflasks.journal.flask.heal",
                percent(shown.healPercentage())));
        line(text, I18n.format("everfillingflasks.journal.flask.recharge",
                seconds(shown.rechargeTicks())));
        line(text, I18n.format("everfillingflasks.journal.flask.drink",
                seconds(shown.drinkTicks())));
        line(text, I18n.format("everfillingflasks.journal.flask.threshold",
                number(shown.hitThreshold())));
        line(text, I18n.format("everfillingflasks.journal.flask.potency",
                Math.max(0, definition.potency(stack, viewer))));
        text.append(PARAGRAPH).append(sourceLine(item));

        JsonArray pages = new JsonArray();
        pages.add(textPage(text.toString()));
        if (recipeName != null) {
            pages.add(craftingPage(recipeName));
        }
        String hint = hint(item, () -> definition.journalHint(stack));
        if (hint != null) {
            pages.add(textPage(hint));
        }
        return entry(stack.getDisplayName(), item, categoryId, pages);
    }

    /** An ingredient entry: its cost, whatever it contributes, its recipe if any, its source. */
    static JsonObject ingredient(Item item, IngredientDefinition definition, EntityPlayer viewer,
            String categoryId, String recipeName) {
        ItemStack stack = new ItemStack(item);
        StringBuilder text = new StringBuilder();
        describe(text, definition.journalDescription(stack));
        line(text, I18n.format("everfillingflasks.journal.ingredient.cost",
                definition.potencyCost(stack)));
        text.append(BREAK).append(effects(stack, definition, viewer));
        text.append(PARAGRAPH).append(sourceLine(item));

        JsonArray pages = new JsonArray();
        pages.add(textPage(text.toString()));
        if (recipeName != null) {
            pages.add(craftingPage(recipeName));
        }
        String hint = hint(item, () -> definition.journalHint(stack));
        if (hint != null) {
            pages.add(textPage(hint));
        }
        return entry(stack.getDisplayName(), item, categoryId, pages);
    }

    /**
     * What an ingredient does, asked of the ingredient rather than assumed.
     *
     * <p>The definition is invited to contribute into an empty accumulator, and whatever lands
     * there is described. An ingredient whose whole effect happens after a drink contributes
     * nothing measurable here, so it gets the general line instead of a wrong one. This is why
     * the journal does not need to know the list of effect kinds (REQ-039).
     */
    private static String effects(ItemStack stack, IngredientDefinition definition,
            EntityPlayer viewer) {
        FlaskBonuses bonuses = new FlaskBonuses();
        try {
            definition.contribute(stack, viewer, bonuses);
        } catch (Throwable failure) {
            return I18n.format("everfillingflasks.journal.ingredient.unknown");
        }
        StringBuilder effects = new StringBuilder();
        appendBonus(effects, "healing", bonuses.healingSum());
        appendBonus(effects, "drinkSpeed", bonuses.drinkSpeedSum());
        appendBonus(effects, "hitThreshold", bonuses.hitResistanceSum());
        appendBonus(effects, "rechargeSpeed", bonuses.rechargeSpeedSum());
        if (bonuses.maxChargesFlat() != 0) {
            line(effects, I18n.format("everfillingflasks.journal.ingredient.maxCharges",
                    bonuses.maxChargesFlat()));
        }
        if (effects.length() == 0) {
            return I18n.format("everfillingflasks.journal.ingredient.unknown");
        }
        return effects.toString();
    }

    private static void appendBonus(StringBuilder text, String key, float sum) {
        if (sum != 0.0F) {
            line(text, I18n.format("everfillingflasks.journal.ingredient." + key, percent(sum)));
        }
    }

    /** The optional paragraph a mod may add above the derived values; absent by default. */
    private static void describe(StringBuilder text, String descriptionKey) {
        if (descriptionKey != null && !descriptionKey.isEmpty()) {
            text.append(I18n.format(descriptionKey)).append(PARAGRAPH);
        }
    }

    /**
     * The "Where to Find" page, or null when there is nothing to say.
     *
     * <p>The pack author is asked first, because only they know what their pack did with this
     * content: their text replaces the mod's, and their empty text hides the page entirely. With
     * no override, the content's own hint stands, and with neither there is no page. A recipe is
     * never treated as an answer to this question (REQ-041, and the recipe-versus-acquisition
     * separation the owner's document asks for).
     */
    private static String hint(Item item, Supplier<String> ownHintKey) {
        ResourceLocation name = item.getRegistryName();
        JournalHintOverrides overrides = ConfigSnapshot.current().hintOverrides();
        String body = null;
        if (name != null && overrides.has(name.toString())) {
            // Asked before the content's own hint, and the content is then never asked at all.
            // A pack author correcting a hint must still be obeyed when the mod's own hint is
            // what is broken, which is the case that most needs correcting (REQ-041).
            String override = overrides.text(name.toString());
            if (override.isEmpty()) {
                return null;
            }
            body = override;
        } else {
            String hintKey = ownHintKey.get();
            if (hintKey != null && !hintKey.isEmpty()) {
                body = I18n.format(hintKey);
            }
        }
        if (body == null) {
            return null;
        }
        return I18n.format("everfillingflasks.journal.whereToFind") + PARAGRAPH + body;
    }

    /** "Added by: <mod name>", secondary information and never navigation (REQ-011 of the doc). */
    private static String sourceLine(Item item) {
        ResourceLocation name = item.getRegistryName();
        String namespace = name == null ? "minecraft" : name.getNamespace();
        ModContainer mod = Loader.instance().getIndexedModList().get(namespace);
        return I18n.format("everfillingflasks.journal.source",
                mod == null ? namespace : mod.getName());
    }

    private static JsonObject entry(String displayName, Item item, String categoryId,
            JsonArray pages) {
        JsonObject json = new JsonObject();
        json.addProperty("name", displayName);
        json.addProperty("category", categoryId);
        json.addProperty("icon", String.valueOf(item.getRegistryName()));
        json.addProperty("read_by_default", true);
        json.add("pages", pages);
        return json;
    }

    private static JsonObject textPage(String text) {
        JsonObject page = new JsonObject();
        page.addProperty("type", "text");
        page.addProperty("text", text);
        return page;
    }

    private static JsonObject craftingPage(String recipeName) {
        JsonObject page = new JsonObject();
        page.addProperty("type", "crafting");
        page.addProperty("recipe", recipeName);
        return page;
    }

    private static void line(StringBuilder text, String content) {
        if (text.length() > 0) {
            text.append(BREAK);
        }
        text.append(content);
    }

    /** 0.33 reads as 33, the same rounding the item tooltip uses. */
    private static int percent(float fraction) {
        return Math.round(fraction * 100.0F);
    }

    /** Ticks as seconds, with a decimal only when there is one: "30", "1.5". */
    private static String seconds(int ticks) {
        return number((float) ticks / TICKS_PER_SECOND);
    }

    private static String number(float value) {
        if (value == Math.round(value)) {
            return String.valueOf(Math.round(value));
        }
        return String.format("%.1f", value);
    }
}
