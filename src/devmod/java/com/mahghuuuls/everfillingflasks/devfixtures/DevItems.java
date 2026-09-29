package com.mahghuuuls.everfillingflasks.devfixtures;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The fixture items' registration. The items always exist in the development runtime; whether
 * they ARE a Flask or an infusion is decided later, in the fixture mod's init, by the
 * property-gated API calls, which is the point: registration timing and item ownership are the
 * add-on's business.
 */
@Mod.EventBusSubscriber(modid = DevFixturesMod.MOD_ID)
public final class DevItems {

    private static FixtureManaFlask manaFlask;
    private static FixtureTradeoffHerb tradeoffHerb;

    private DevItems() {
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        manaFlask = new FixtureManaFlask();
        tradeoffHerb = new FixtureTradeoffHerb();
        event.getRegistry().register(manaFlask);
        event.getRegistry().register(tradeoffHerb);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onRegisterModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(manaFlask, 0,
                new ModelResourceLocation(manaFlask.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(tradeoffHerb, 0,
                new ModelResourceLocation(tradeoffHerb.getRegistryName(), "inventory"));
    }

    public static FixtureManaFlask manaFlask() {
        return manaFlask;
    }

    public static FixtureTradeoffHerb tradeoffHerb() {
        return tradeoffHerb;
    }
}
