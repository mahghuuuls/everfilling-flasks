package com.mahghuuuls.everfillingflasks.client;

import com.mahghuuuls.everfillingflasks.Tags;
import com.mahghuuuls.everfillingflasks.item.FlaskTier;
import com.mahghuuuls.everfillingflasks.item.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Item model registration for the four Flasks. */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Tags.MOD_ID)
public final class ClientModels {

    private ClientModels() {
    }

    @SubscribeEvent
    public static void onRegisterModels(ModelRegistryEvent event) {
        for (FlaskTier tier : FlaskTier.values()) {
            ModelLoader.setCustomModelResourceLocation(ModItems.flask(tier), 0,
                    new ModelResourceLocation(ModItems.flask(tier).getRegistryName(), "inventory"));
        }
        for (com.mahghuuuls.everfillingflasks.item.IngredientKind kind
                : com.mahghuuuls.everfillingflasks.item.IngredientKind.values()) {
            ModelLoader.setCustomModelResourceLocation(ModItems.ingredient(kind), 0,
                    new ModelResourceLocation(
                            ModItems.ingredient(kind).getRegistryName(), "inventory"));
        }
    }
}
