package com.mahghuuuls.everfillingflasks.world;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.plant.WildPlant;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Each plant's biome rule checked against the installed game, once every mod has registered its
 * biomes (post-init). Unknown type and biome names are warned about once each and then match
 * nothing. Generation and the journal both read the resolved set of matching biomes, so they
 * agree on where a plant grows.
 */
public final class ResolvedBiomeRules {

    private static volatile Map<WildPlant, Set<Biome>> matching =
            Collections.unmodifiableMap(new EnumMap<WildPlant, Set<Biome>>(WildPlant.class));

    private ResolvedBiomeRules() {
    }

    /** Resolves every plant's rule from the current configuration. Called once, at post-init. */
    public static void resolveAll() {
        Set<String> knownTypes = new HashSet<String>();
        for (BiomeDictionary.Type type : BiomeDictionary.Type.getAll()) {
            knownTypes.add(type.getName());
        }
        Map<WildPlant, Set<Biome>> resolved = new EnumMap<WildPlant, Set<Biome>>(WildPlant.class);
        for (WildPlant plant : WildPlant.values()) {
            ConfigSnapshot.PlantSettings settings = ConfigSnapshot.current().plant(plant.key());
            BiomeRule rule = BiomeRule.of(settings.biomeTypes(), settings.biomeNames());
            warnUnknown(plant, rule, knownTypes);
            Set<Biome> biomes = new HashSet<Biome>();
            for (Biome biome : ForgeRegistries.BIOMES.getValuesCollection()) {
                if (matches(rule, biome)) {
                    biomes.add(biome);
                }
            }
            resolved.put(plant, Collections.unmodifiableSet(biomes));
        }
        matching = Collections.unmodifiableMap(resolved);
    }

    /** Whether a plant may grow in this biome. False for everything before post-init. */
    public static boolean allows(WildPlant plant, Biome biome) {
        Set<Biome> biomes = matching.get(plant);
        return biomes != null && biomes.contains(biome);
    }

    /** The installed biomes a plant may grow in. Empty before post-init. */
    public static Set<Biome> biomes(WildPlant plant) {
        Set<Biome> biomes = matching.get(plant);
        return biomes == null ? Collections.<Biome>emptySet() : biomes;
    }

    private static boolean matches(BiomeRule rule, Biome biome) {
        ResourceLocation name = biome.getRegistryName();
        Set<String> types = new HashSet<String>();
        for (BiomeDictionary.Type type : BiomeDictionary.getTypes(biome)) {
            types.add(type.getName());
        }
        return rule.matches(name == null ? "" : name.toString(), types);
    }

    private static void warnUnknown(WildPlant plant, BiomeRule rule, Set<String> knownTypes) {
        List<String> unknown = new ArrayList<String>();
        for (String type : rule.typeNames()) {
            if (!knownTypes.contains(type)) {
                unknown.add("biome type " + type);
            }
        }
        for (String name : rule.biomeNames()) {
            if (!ForgeRegistries.BIOMES.containsKey(new ResourceLocation(name))) {
                unknown.add("biome " + name);
            }
        }
        for (String entry : unknown) {
            EverfillingFlasksMod.LOGGER.warn("plants.{}: {} is not installed; ignored", plant.key(), entry);
        }
    }
}