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
 * The fixture item's registration. The item always exists in the development runtime; whether
 * it IS a Flask is decided later, in the fixture mod's init, by the property-gated API call —
 * which is the point: registration timing and item ownership are the add-on's business.
 */
@Mod.EventBusSubscriber(modid = DevFixturesMod.MOD_ID)
public final class DevItems {

    private static FixtureManaFlask manaFlask;

    private DevItems() {
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        manaFlask = new FixtureManaFlask();
        event.getRegistry().register(manaFlask);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onRegisterModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(manaFlask, 0,
                new ModelResourceLocation(manaFlask.getRegistryName(), "inventory"));
    }

    public static FixtureManaFlask manaFlask() {
        return manaFlask;
    }
}
