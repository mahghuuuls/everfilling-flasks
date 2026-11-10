package com.mahghuuuls.everfillingflasks.client.journal;

import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.plant.WildPlant;
import com.mahghuuuls.everfillingflasks.world.BiomeRule;
import com.mahghuuuls.everfillingflasks.world.ResolvedBiomeRules;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What the journal says about where an item grows, for the items a wild plant gives: that it
 * grows wild in its configured biome types (as words from the language file, such as "forests")
 * and its configured biome names, anywhere for {@code *}, not at all when its plant cannot grow,
 * and that chests hold it too while chest loot is on. Every word, list separator, and sentence
 * join comes from the language file; biome names are the game's own, which 1.12.2 does not
 * translate.
 */
@SideOnly(Side.CLIENT)
final class PlantJournalText {

    private static final String KEY = "everfillingflasks.journal.text.wild.";

    /** How the text is phrased, chosen before any word is looked up. */
    enum Kind {
        BIOMES, EVERYWHERE, NONE
    }

    /** Turns a language key and its arguments into text; the game's translator, or a test's. */
    interface Words {
        String format(String key, Object... args);
    }

    /** The game's translator. */
    static final Words GAME_WORDS = new Words() {
        @Override
        public String format(String key, Object... args) {
            return I18n.format(key, args);
        }
    };

    private PlantJournalText() {
    }

    /** The localized text for a plant's item. */
    static String describe(WildPlant plant) {
        ConfigSnapshot config = ConfigSnapshot.current();
        ConfigSnapshot.PlantSettings settings = config.plant(plant.key());
        BiomeRule rule = BiomeRule.of(settings.biomeTypes(), settings.biomeNames());
        Words words = GAME_WORDS;
        List<String> places = new ArrayList<String>();
        Set<String> installedTypes = installedTypeNames(ResolvedBiomeRules.biomes(plant));
        for (String type : rule.typeNames()) {
            // Only types some installed Overworld biome has: a type nothing has would name
            // places a player cannot find.
            if (installedTypes.contains(type)) {
                places.add(typeWord(type));
            }
        }
        for (String name : rule.biomeNames()) {
            Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(name));
            if (biome != null && biome.getBiomeName() != null && !biome.getBiomeName().isEmpty()) {
                places.add(biome.getBiomeName());
            }
        }
        Kind kind = kind(settings.enabled(), settings.chancePercent(), rule.matchesEveryBiome(),
                !ResolvedBiomeRules.biomes(plant).isEmpty());
        return text(kind, places, config.infusionLoot(), words);
    }

    /** Which phrasing applies. A plant that cannot grow anywhere says so rather than list places. */
    static Kind kind(boolean enabled, int chancePercent, boolean everywhere, boolean anyBiomeInstalled) {
        if (!enabled || chancePercent <= 0 || !anyBiomeInstalled) {
            return Kind.NONE;
        }
        return everywhere ? Kind.EVERYWHERE : Kind.BIOMES;
    }

    /** The text for a phrasing, its places, and whether chest loot is on. */
    static String text(Kind kind, List<String> places, boolean chestLoot, Words words) {
        String suffix = chestLoot ? ".chests" : "";
        switch (kind) {
            case EVERYWHERE:
                return words.format(KEY + "everywhere" + suffix);
            case BIOMES:
                return words.format(KEY + "biomes" + suffix, list(places, words));
            default:
                return words.format(KEY + "none" + suffix);
        }
    }

    /** Places joined as the language joins a list: "a", "a and b", "a, b and c". */
    static String list(List<String> places, Words words) {
        if (places.isEmpty()) {
            return "";
        }
        String joined = places.get(0);
        for (int i = 1; i < places.size(); i++) {
            joined = words.format(KEY + (i == places.size() - 1 ? "list.last" : "list.next"), joined, places.get(i));
        }
        return joined;
    }

    /** The dictionary type names the given biomes have, as the dictionary writes them. */
    private static Set<String> installedTypeNames(Set<Biome> biomes) {
        Set<String> names = new HashSet<String>();
        for (Biome biome : biomes) {
            for (BiomeDictionary.Type type : BiomeDictionary.getTypes(biome)) {
                names.add(type.getName());
            }
        }
        return names;
    }

    /** The item's own text followed by the wild text, joined as the language joins them. */
    static String withOwnText(String own, String wild, Words words) {
        return own.isEmpty() ? wild : words.format(KEY + "after", own, wild);
    }

    /** The word for a biome dictionary type; a type with no word, from another mod, as its own name. */
    private static String typeWord(String type) {
        String key = "everfillingflasks.biometype." + type.toLowerCase(Locale.ROOT);
        return I18n.hasKey(key) ? I18n.format(key) : type.toLowerCase(Locale.ROOT);
    }
}