package com.mahghuuuls.everfillingflasks.plant;

import com.mahghuuuls.everfillingflasks.Tags;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.EnumMap;
import java.util.Map;

/**
 * The five wild plant blocks and the stem tile entity. Blocks only: a wild plant has no item
 * block, because players find plants and never place them.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModBlocks {

    private static final Map<WildPlant, BlockWildPlant> PLANTS =
            new EnumMap<WildPlant, BlockWildPlant>(WildPlant.class);

    private ModBlocks() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        for (WildPlant plant : WildPlant.values()) {
            BlockWildPlant block = new BlockWildPlant(plant);
            PLANTS.put(plant, block);
            event.getRegistry().register(block);
        }
        GameRegistry.registerTileEntity(TileEntityStem.class, new ResourceLocation(Tags.MOD_ID, "stem"));
    }

    /** The block of a plant. Null before block registration. */
    public static BlockWildPlant plant(WildPlant plant) {
        return PLANTS.get(plant);
    }
}
