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
        // The kind header first, green like the infusions' own, so any tier reads as an
        // Everfilling Flask at a glance. The rest is in the owner's reading order (2026-08-26):
        // what it does for you, then what it costs you, then how to use it.
        tooltip.add(net.minecraft.util.text.TextFormatting.GREEN
                + I18n.format("everfillingflasks.tooltip.flaskHeader"));
        ConfigSnapshot.TierConfig values = ConfigSnapshot.current().tier(tier);
        tooltip.add(I18n.format("everfillingflasks.tooltip.heals",
                Math.round(values.healPercentage() * 100.0F)));
        // Maximum only, owner decision 2026-08-25: current charges are the HUD's job.
        tooltip.add(I18n.format("everfillingflasks.tooltip.charges", values.maxCharges()));
        tooltip.add(I18n.format("everfillingflasks.tooltip.potency", values.potency()));
        tooltip.add(I18n.format("everfillingflasks.tooltip.useTime",
                seconds(values.drinkTicks())));
        tooltip.add(I18n.format("everfillingflasks.tooltip.threshold",
                number(values.hitThreshold())));
        tooltip.add(I18n.format("everfillingflasks.tooltip.recharge",
                seconds(values.rechargeTicks())));
        // Only where it can actually happen: a player without Inhibited installed should not
        // be told about a freeze their game has no way to cause (owner decision 2026-08-26).
        if (com.mahghuuuls.everfillingflasks.integration.InhibitedCompat.isAvailable()) {
            tooltip.add(I18n.format("everfillingflasks.tooltip.inhibited"));
        }
        tooltip.add(I18n.format("everfillingflasks.tooltip.usage",
                EverfillingFlasksMod.proxy.useFlaskKeyName()));
        addInfusionLines(stack, tooltip);
    }

    /** Ticks as seconds, with a decimal only when there is one: "30", "1.5". */
    private static String seconds(int ticks) {
        return number(ticks / 20.0F);
    }

    /** A number with a decimal only when it has one: "1", "2.5". */
    private static String number(float value) {
        return value == Math.round(value) ? String.valueOf(Math.round(value))
                : String.format("%.1f", value);
    }

    /**
     * The infused infusions, one line per distinct kind with a count. Owner-requested so a
     * player sees what a Flask carries — and what an upgrade craft would consume — without
     * opening the screen. Reads the stack's own grid, so it is right wherever the tooltip
     * shows: inventory, chest, or the crafting table.
     */
    private static void addInfusionLines(ItemStack stack, List<String> tooltip) {
        // Each piece keeps the colour its own name has in an inventory, so a rare infusion
        // stands out in the list exactly as the item does (owner decision 2026-08-26).
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<String, Integer>();
        for (ItemStack piece : FlaskStackState.infusions(stack)) {
            if (!piece.isEmpty()) {
                String name = piece.getRarity().getColor() + piece.getDisplayName();
                Integer previous = counts.get(name);
                counts.put(name, previous == null ? 1 : previous + 1);
            }
        }
        if (counts.isEmpty()) {
            return;
        }
        tooltip.add(net.minecraft.util.text.TextFormatting.GREEN
                + I18n.format("everfillingflasks.tooltip.infused"));
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            tooltip.add(I18n.format("everfillingflasks.tooltip.infusedLine",
                    entry.getKey(), entry.getValue()));
        }
    }
}
