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
        int maxCharges = ConfigSnapshot.current().tier(tier).maxCharges();
        tooltip.add(I18n.format("everfillingflasks.tooltip.charges",
                FlaskStackState.charges(stack), maxCharges));
        tooltip.add(I18n.format("everfillingflasks.tooltip.usage",
                EverfillingFlasksMod.proxy.useFlaskKeyName()));
    }
}
