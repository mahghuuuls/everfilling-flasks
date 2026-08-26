package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.Tags;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.EnumMap;
import java.util.Map;

/**
 * The four built-in Flask items: their registration, their creative tab, and their Flask
 * definitions. Items are created once at registry time and looked up by tier afterwards.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModItems {

    private static final Map<FlaskTier, ItemFlask> FLASKS =
            new EnumMap<FlaskTier, ItemFlask>(FlaskTier.class);
    private static final Map<InfusionKind, ItemInfusion> INFUSIONS =
            new EnumMap<InfusionKind, ItemInfusion>(InfusionKind.class);

    public static final CreativeTabs TAB = new CreativeTabs(Tags.MOD_ID) {
        @SideOnly(Side.CLIENT)
        @Override
        public ItemStack createIcon() {
            return new ItemStack(flask(FlaskTier.COMMON));
        }
    };

    private ModItems() {
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (FlaskTier tier : FlaskTier.values()) {
            ItemFlask item = new ItemFlask(tier);
            item.setCreativeTab(TAB);
            FLASKS.put(tier, item);
            event.getRegistry().register(item);
            FlaskRegistry.register(item, new TierFlaskDefinition(tier));
        }
        for (InfusionKind kind : InfusionKind.values()) {
            ItemInfusion item = new ItemInfusion(kind);
            item.setCreativeTab(TAB);
            INFUSIONS.put(kind, item);
            event.getRegistry().register(item);
            // Directly into the internal registry, like the Flasks: the public API's bridge
            // exists for add-ons, not for the mod's own content.
            com.mahghuuuls.everfillingflasks.flask.InfusionRegistry.register(item,
                    new BuiltinInfusionDefinition(kind));
        }
    }

    public static ItemFlask flask(FlaskTier tier) {
        return FLASKS.get(tier);
    }

    public static ItemInfusion infusion(InfusionKind kind) {
        return INFUSIONS.get(kind);
    }
}
