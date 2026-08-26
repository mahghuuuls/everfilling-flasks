package com.mahghuuuls.everfillingflasks.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The recipe files parse and carry the shape the loader needs: the condition naming this mod's
 * factory with the right switch key, and a result naming the matching item. A malformed file
 * would otherwise only fail at game start, which the compile check cannot see.
 */
class RecipeJsonTest {

    private static JsonObject load(String name) {
        InputStream stream = RecipeJsonTest.class.getClassLoader()
                .getResourceAsStream("assets/everfillingflasks/recipes/" + name);
        assertNotNull(stream, name + " must be on the classpath");
        return new JsonParser().parse(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void noInfusionRecipeFilesRemain() {
        // Infusions come from treasure chests (owner decision 2026-08-25); a leftover
        // recipe file would quietly bring crafting back.
        for (String key : new String[]{"sunpetal_leaf", "ironroot_sprig", "quickmint_leaf",
                "second_wind_petal", "sunmelon_shard", "ironbark_chip", "quicksilver_drop"}) {
            org.junit.jupiter.api.Assertions.assertNull(RecipeJsonTest.class.getClassLoader()
                            .getResource("assets/everfillingflasks/recipes/" + key + ".json"),
                    key + " must have no recipe file");
        }
    }

    private static void checkFlaskRecipe(String tier, int infusionCount) {
        JsonObject recipe = load(tier + "_flask.json");
        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
        JsonObject condition = recipe.getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals("everfillingflasks:recipe_enabled", condition.get("type").getAsString());
        assertEquals(tier, condition.get("recipe").getAsString());
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("everfillingflasks:" + tier + "_flask", result.get("item").getAsString());
        // No NBT on the output: a crafted Flask reads 0 charges by the persistence rule.
        org.junit.jupiter.api.Assertions.assertFalse(result.has("nbt"));
        assertEquals(infusionCount, recipe.getAsJsonArray("ingredients").size());
    }

    @Test
    void everyFlaskRecipeParsesWithItsSwitchAndAnEmptyOutput() {
        checkFlaskRecipe("common", 4);
        checkFlaskRecipe("uncommon", 4);
        checkFlaskRecipe("rare", 4);
    }

    @Test
    void theFactoryFileNamesTheConditionClass() {
        JsonObject factories = load("_factories.json");
        assertEquals(RecipeEnabledCondition.class.getName(),
                factories.getAsJsonObject("conditions").get("recipe_enabled").getAsString());
    }
    @Test
    void theHumbleFlaskIsBuiltOnAnEverlastingSeed() {
        // The seed is the whole fiction: a Flask refills itself because of what is inside it.
        // A recipe that quietly lost the seed would leave the story without its cause.
        JsonObject recipe = load("common_flask.json");
        boolean hasSeed = false;
        for (int i = 0; i < recipe.getAsJsonArray("ingredients").size(); i++) {
            String item = recipe.getAsJsonArray("ingredients").get(i).getAsJsonObject()
                    .get("item").getAsString();
            if ("everfillingflasks:everlasting_seed".equals(item)) {
                hasSeed = true;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(hasSeed,
                "the Humble Flask recipe must contain an Everlasting Seed");
    }

    @Test
    void noSeedRecipeFileExists() {
        // Seeds are found, never made (REQ-044).
        org.junit.jupiter.api.Assertions.assertNull(RecipeJsonTest.class.getClassLoader()
                        .getResource("assets/everfillingflasks/recipes/everlasting_seed.json"),
                "the Everlasting Seed must have no recipe");
    }

}
