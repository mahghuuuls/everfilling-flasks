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

    private static void checkIngredientRecipe(String key) {
        JsonObject recipe = load(key + ".json");
        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());

        JsonArray conditions = recipe.getAsJsonArray("conditions");
        assertEquals(1, conditions.size());
        JsonObject condition = conditions.get(0).getAsJsonObject();
        assertEquals("everfillingflasks:recipe_enabled", condition.get("type").getAsString());
        assertEquals(key, condition.get("recipe").getAsString(),
                "the switch key must match the config's recipes entry");

        assertEquals("everfillingflasks:" + key,
                recipe.getAsJsonObject("result").get("item").getAsString());
        assertTrue(recipe.getAsJsonArray("ingredients").size() >= 2);
    }

    @Test
    void everyIngredientRecipeParsesWithItsSwitch() {
        checkIngredientRecipe("sunmelon_shard");
        checkIngredientRecipe("ironbark_chip");
        checkIngredientRecipe("quicksilver_drop");
        checkIngredientRecipe("second_wind_petal");
    }

    @Test
    void theFactoryFileNamesTheConditionClass() {
        JsonObject factories = load("_factories.json");
        assertEquals(RecipeEnabledCondition.class.getName(),
                factories.getAsJsonObject("conditions").get("recipe_enabled").getAsString());
    }
}
