package com.mahghuuuls.everfillingflasks.client.journal;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How the journal phrases where an item grows, checked against the shipped English file. */
class PlantJournalTextTest {

    private static Map<String, String> english() throws Exception {
        Map<String, String> lang = new HashMap<String, String>();
        InputStream in = PlantJournalTextTest.class.getResourceAsStream("/assets/everfillingflasks/lang/en_us.lang");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            int eq = line.indexOf('=');
            if (eq > 0 && !line.startsWith("#")) {
                lang.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        reader.close();
        return lang;
    }

    /** Formats with the shipped English strings and records every key asked for. */
    private static final class English implements PlantJournalText.Words {
        final Map<String, String> lang;
        final List<String> asked = new ArrayList<String>();

        English(Map<String, String> lang) {
            this.lang = lang;
        }

        @Override
        public String format(String key, Object... args) {
            asked.add(key);
            String pattern = lang.get(key);
            return pattern == null ? key : String.format(pattern, args);
        }
    }

    @Test
    void anItemsOwnTextComesFirstAndAnEmptyOneIsLeftOut() throws Exception {
        English words = new English(english());
        assertEquals("Lore. Wild.", PlantJournalText.withOwnText("Lore.", "Wild.", words));
        assertEquals("Wild.", PlantJournalText.withOwnText("", "Wild.", words));
    }

    @Test
    void theKindFollowsTheSettings() {
        assertEquals(PlantJournalText.Kind.BIOMES, PlantJournalText.kind(true, 3, false, true));
        assertEquals(PlantJournalText.Kind.EVERYWHERE, PlantJournalText.kind(true, 3, true, true));
        assertEquals(PlantJournalText.Kind.NONE, PlantJournalText.kind(false, 3, false, true));
        assertEquals(PlantJournalText.Kind.NONE, PlantJournalText.kind(true, 0, false, true));
        assertEquals(PlantJournalText.Kind.NONE, PlantJournalText.kind(true, 3, false, false));
    }

    @Test
    void theBiomeTextNamesThePlacesInOneSentence() throws Exception {
        English words = new English(english());
        String text = PlantJournalText.text(PlantJournalText.Kind.BIOMES,
                Arrays.asList("forests", "jungles", "swamps"), false, words);
        assertTrue(text.startsWith("It grows wild, alone, in forests, jungles and swamps."), text);
        assertTrue(!text.contains("chests"), text);
    }

    @Test
    void chestsAreMentionedOnlyWhileChestLootIsOn() throws Exception {
        English words = new English(english());
        assertTrue(PlantJournalText.text(PlantJournalText.Kind.EVERYWHERE, Collections.<String>emptyList(), true, words)
                .contains("chests"));
        assertTrue(PlantJournalText.text(PlantJournalText.Kind.NONE, Collections.<String>emptyList(), true, words)
                .contains("chests"));
        assertTrue(!PlantJournalText.text(PlantJournalText.Kind.NONE, Collections.<String>emptyList(), false, words)
                .contains("chests"));
    }

    @Test
    void listsJoinAsTheLanguageSays() throws Exception {
        English words = new English(english());
        assertEquals("plains", PlantJournalText.list(Arrays.asList("plains"), words));
        assertEquals("plains and savannas", PlantJournalText.list(Arrays.asList("plains", "savannas"), words));
        assertEquals("a, b, c and d", PlantJournalText.list(Arrays.asList("a", "b", "c", "d"), words));
    }

    @Test
    void everyKeyTheTextCanAskForIsInTheEnglishFile() throws Exception {
        Map<String, String> lang = english();
        English words = new English(lang);
        for (PlantJournalText.Kind kind : PlantJournalText.Kind.values()) {
            for (boolean loot : new boolean[] {true, false}) {
                PlantJournalText.text(kind, Arrays.asList("a", "b", "c"), loot, words);
            }
        }
        PlantJournalText.withOwnText("own", "wild", words);
        for (String key : words.asked) {
            assertTrue(lang.containsKey(key), key);
        }
        for (net.minecraftforge.common.BiomeDictionary.Type type : net.minecraftforge.common.BiomeDictionary.Type.getAll()) {
            String key = "everfillingflasks.biometype." + type.getName().toLowerCase(java.util.Locale.ROOT);
            assertTrue(lang.containsKey(key), key);
        }
    }
}