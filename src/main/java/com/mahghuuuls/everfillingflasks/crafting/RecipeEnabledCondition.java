package com.mahghuuuls.everfillingflasks.crafting;

import com.google.gson.JsonObject;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.function.BooleanSupplier;

/**
 * The recipe switch: {@code {"type": "everfillingflasks:recipe_enabled", "recipe": "<name>"}}
 * in a recipe's conditions, where the name is a tier or infusion key from the config's
 * recipes block. Conditions are evaluated when recipes load, after the config was read at
 * pre-initialization, which is exactly the "next game start" contract every config comment
 * promises. An unknown name reads as enabled, by the snapshot's rule, so a typo cannot
 * silently remove content.
 */
public final class RecipeEnabledCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(JsonContext context, JsonObject json) {
        final String recipe = JsonUtils.getString(json, "recipe");
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return ConfigSnapshot.current().recipeEnabled(recipe);
            }
        };
    }
}
