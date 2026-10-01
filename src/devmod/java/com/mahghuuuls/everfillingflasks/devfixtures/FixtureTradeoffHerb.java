package com.mahghuuuls.everfillingflasks.devfixtures;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A fixture infusion item that writes one line of its own, so the campaign can see the core's
 * standard lines come first and this one after them (REQ-053).
 */
public final class FixtureTradeoffHerb extends Item {

    public FixtureTradeoffHerb() {
        setRegistryName(DevFixturesMod.MOD_ID, "tradeoff_herb");
        setTranslationKey("everfillingflasksdev.tradeoff_herb");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        tooltip.add("Fixture: this line is the add-on's own");
    }
}
