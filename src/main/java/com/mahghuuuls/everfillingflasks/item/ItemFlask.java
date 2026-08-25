package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * One built-in Flask. The item is deliberately thin: charges live on the stack, numbers live in
 * the config, behavior lives in the player systems. It exists so the four tiers have registry
 * identities, rarity colors, and a tooltip that teaches the key.
 */
public final class ItemFlask extends Item {

    private final FlaskTier tier;

    public ItemFlask(FlaskTier tier) {
        this.tier = tier;
        setMaxStackSize(1);
        setRegistryName(tier.registryName());
        setTranslationKey("everfillingflasks." + tier.registryName());
    }

    public FlaskTier tier() {
        return tier;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return tier.rarity();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        // The kind header first, green like the ingredients' own, so any tier reads as an
        // Everfilling Flask at a glance.
        tooltip.add(net.minecraft.util.text.TextFormatting.GREEN
                + I18n.format("everfillingflasks.tooltip.flaskHeader"));
        // Maximum only, owner decision 2026-08-25: current charges are the HUD's job.
        tooltip.add(I18n.format("everfillingflasks.tooltip.charges",
                ConfigSnapshot.current().tier(tier).maxCharges()));
        tooltip.add(I18n.format("everfillingflasks.tooltip.heals", Math.round(
                ConfigSnapshot.current().tier(tier).healPercentage() * 100.0F)));
        tooltip.add(I18n.format("everfillingflasks.tooltip.usage",
                EverfillingFlasksMod.proxy.useFlaskKeyName()));
        addInfusionLines(stack, tooltip);
    }

    /**
     * The infused ingredients, one line per distinct kind with a count. Owner-requested so a
     * player sees what a Flask carries — and what an upgrade craft would consume — without
     * opening the screen. Reads the stack's own grid, so it is right wherever the tooltip
     * shows: inventory, chest, or the crafting table.
     */
    private static void addInfusionLines(ItemStack stack, List<String> tooltip) {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<String, Integer>();
        for (ItemStack piece : FlaskStackState.ingredients(stack)) {
            if (!piece.isEmpty()) {
                String name = piece.getDisplayName();
                Integer previous = counts.get(name);
                counts.put(name, previous == null ? 1 : previous + 1);
            }
        }
        if (counts.isEmpty()) {
            return;
        }
        tooltip.add(I18n.format("everfillingflasks.tooltip.infused"));
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            tooltip.add(I18n.format("everfillingflasks.tooltip.infusedLine",
                    entry.getKey(), entry.getValue()));
        }
    }
}
