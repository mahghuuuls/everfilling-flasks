package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * One built-in Flask Ingredient. As thin as the Flasks: behavior lives in the registered
 * definition, numbers live in the config, and the item exists for registry identity and a
 * tooltip that states cost and effect in the owner's wording ("hit threshold").
 */
public final class ItemIngredient extends Item {

    private final IngredientKind kind;

    public ItemIngredient(IngredientKind kind) {
        this.kind = kind;
        setRegistryName(kind.key());
        setTranslationKey("everfillingflasks." + kind.key());
    }

    public IngredientKind kind() {
        return kind;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        ConfigSnapshot.IngredientConfig config = ConfigSnapshot.current().ingredient(kind);
        tooltip.add(I18n.format("everfillingflasks.tooltip.ingredient.cost", config.cost()));
        int percent = (int) Math.round(config.strength() * 100.0);
        switch (kind.effect()) {
            case HEALING:
                tooltip.add(I18n.format("everfillingflasks.tooltip.ingredient.healing", percent));
                break;
            case HIT_THRESHOLD:
                tooltip.add(I18n.format(
                        "everfillingflasks.tooltip.ingredient.hitThreshold", percent));
                break;
            case DRINK_SPEED:
                tooltip.add(I18n.format(
                        "everfillingflasks.tooltip.ingredient.drinkSpeed", percent));
                break;
            case POST_DRINK_REGEN:
                tooltip.add(I18n.format("everfillingflasks.tooltip.ingredient.regen",
                        (int) Math.round(config.strength())));
                break;
        }
    }
}
