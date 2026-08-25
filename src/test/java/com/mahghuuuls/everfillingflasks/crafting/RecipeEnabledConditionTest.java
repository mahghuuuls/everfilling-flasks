package com.mahghuuuls.everfillingflasks.crafting;

import com.google.gson.JsonObject;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.config.FlaskConfig;
import net.minecraftforge.common.crafting.JsonContext;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The condition factory end to end: the parsed supplier answers the live snapshot, so a
 * config flip between parses (impossible in a real session, but the honest way to prove the
 * supplier reads the snapshot and not a captured value) changes the answer.
 */
class RecipeEnabledConditionTest {

    private static BooleanSupplier parse(String recipe) {
        JsonObject json = new JsonObject();
        json.addProperty("recipe", recipe);
        return new RecipeEnabledCondition().parse(new JsonContext("everfillingflasks"), json);
    }

    @Test
    void theSupplierReadsTheSnapshotNotACapturedValue() {
        BooleanSupplier rare = parse("rare");
        assertTrue(rare.getAsBoolean(), "enabled by default");

        boolean original = FlaskConfig.recipes.rare;
        try {
            FlaskConfig.recipes.rare = false;
            ConfigSnapshot.refresh();
            assertFalse(rare.getAsBoolean(), "the same supplier follows the snapshot");
        } finally {
            FlaskConfig.recipes.rare = original;
            ConfigSnapshot.refresh();
        }
    }

    @Test
    void anUnknownNameStaysEnabled() {
        assertTrue(parse("definitely_not_a_recipe").getAsBoolean());
    }
}
