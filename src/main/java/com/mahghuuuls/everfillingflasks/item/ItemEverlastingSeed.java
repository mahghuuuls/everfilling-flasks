package com.mahghuuuls.everfillingflasks.item;

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
 * The Everlasting Seed: the reason a Flask fills itself back up.
 *
 * <p>It does nothing on its own. It is the material every Flask is built around, and the
 * fiction the rest of the mod rests on: a vessel holding these seeds keeps producing a draught
 * shaped by the vessel itself, which is why one Flask heals more than another and why an
 * infusion changes what comes out.
 *
 * <p>Found, never crafted, so a player's first Flask is something the world gave them.
 */
public final class ItemEverlastingSeed extends Item {

    public ItemEverlastingSeed() {
        setRegistryName("everlasting_seed");
        setTranslationKey("everfillingflasks.everlasting_seed");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        tooltip.add(net.minecraft.util.text.TextFormatting.GREEN
                + I18n.format("everfillingflasks.tooltip.seed.header"));
        tooltip.add(I18n.format("everfillingflasks.tooltip.seed.use"));
    }
}
