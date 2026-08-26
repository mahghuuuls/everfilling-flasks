package com.mahghuuuls.everfillingflasks.loot;

import com.mahghuuuls.everfillingflasks.Tags;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.item.InfusionKind;
import com.mahghuuuls.everfillingflasks.item.ModItems;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryEmpty;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Puts the built-in infusions into common exploration chests — the owner's 2026-08-25
 * acquisition decision: found in the world, not crafted. One extra pool on the dungeon,
 * abandoned-mineshaft, and village-blacksmith tables, added at load time through Forge's
 * loot-table event, so datapack-style table replacements from other mods still get the pool.
 *
 * <p>The pool rolls once per chest: usually one to two pieces of one herb, sometimes nothing
 * (the empty entry), with the Second Wind Petal rarer than the percentage herbs. The whole
 * pool is behind {@code general.infusionLoot}; off means no pool is ever added.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class InfusionLootInjector {

    private static final Set<ResourceLocation> TARGETS = new HashSet<ResourceLocation>(
            Arrays.asList(LootTableList.CHESTS_SIMPLE_DUNGEON,
                    LootTableList.CHESTS_ABANDONED_MINESHAFT,
                    LootTableList.CHESTS_VILLAGE_BLACKSMITH));

    private InfusionLootInjector() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!ConfigSnapshot.current().infusionLoot() || !TARGETS.contains(event.getName())) {
            return;
        }
        LootEntry[] entries = new LootEntry[]{
                // The seed comes first and weighs most: a player needs one before any of this
                // matters, so finding one is the start of the whole thing.
                seed(40),
                herb(InfusionKind.SUNPETAL_LEAF, 30),
                herb(InfusionKind.IRONROOT_SPRIG, 30),
                herb(InfusionKind.QUICKMINT_LEAF, 30),
                herb(InfusionKind.SECOND_WIND_PETAL, 15),
                new LootEntryEmpty(45, 0, new LootCondition[0],
                        Tags.MOD_ID + ":nothing"),
        };
        event.getTable().addPool(new LootPool(entries, new LootCondition[0],
                new RandomValueRange(1.0F), new RandomValueRange(0.0F),
                Tags.MOD_ID + "_infusions"));
    }

    private static LootEntryItem seed(int weight) {
        return new LootEntryItem(ModItems.seed(), weight, 0,
                new LootFunction[]{new SetCount(new LootCondition[0],
                        new RandomValueRange(1.0F, 2.0F))},
                new LootCondition[0], Tags.MOD_ID + ":everlasting_seed");
    }

    private static LootEntryItem herb(InfusionKind kind, int weight) {
        return new LootEntryItem(ModItems.infusion(kind), weight, 0,
                new LootFunction[]{new SetCount(new LootCondition[0],
                        new RandomValueRange(1.0F, 2.0F))},
                new LootCondition[0], Tags.MOD_ID + ":" + kind.key());
    }
}
