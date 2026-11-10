package com.mahghuuuls.everfillingflasks.world;

import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.diagnostics.Diagnostics;
import com.mahghuuuls.everfillingflasks.plant.BlockWildPlant;
import com.mahghuuuls.everfillingflasks.plant.ModBlocks;
import com.mahghuuuls.everfillingflasks.plant.WildPlant;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * Places wild plants while new Overworld chunks are decorated. For each enabled plant, one roll
 * against its chance; on success up to {@link #CANDIDATES} random columns, and the plant goes in
 * the first whose biome allows it and whose top stands on the plant's ground. At most one plant
 * of each kind per chunk, never a group.
 *
 * <p>Columns are offset by 8 like vanilla decoration, so placement never reaches into a chunk that
 * is not generated yet.
 */
public final class PlantWorldGenerator implements IWorldGenerator {

    /** Candidate columns tried per successful roll. */
    static final int CANDIDATES = 16;

    private static final long SEED_SALT = 0x6576657266696C6CL;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator,
                         IChunkProvider chunkProvider) {
        if (world.provider.getDimensionType() != DimensionType.OVERWORLD) {
            return;
        }
        // Forge gives every generator the same seeded sequence; a sequence of our own keeps
        // these plants from landing on the same rolls and columns as another mod's.
        Random own = new Random(random.nextLong() ^ SEED_SALT);
        ConfigSnapshot config = ConfigSnapshot.current();
        for (WildPlant plant : WildPlant.values()) {
            ConfigSnapshot.PlantSettings settings = config.plant(plant.key());
            if (!settings.enabled() || own.nextInt(100) >= settings.chancePercent()) {
                continue;
            }
            BlockWildPlant block = ModBlocks.plant(plant);
            for (int attempt = 0; attempt < CANDIDATES; attempt++) {
                int x = chunkX * 16 + 8 + own.nextInt(16);
                int z = chunkZ * 16 + 8 + own.nextInt(16);
                Biome biome = world.getBiome(new BlockPos(x, 0, z));
                if (!ResolvedBiomeRules.allows(plant, biome)) {
                    continue;
                }
                BlockPos pos = ColumnFinder.find(world, x, z);
                if (pos == null || !block.canGrowAt(world, pos)) {
                    continue;
                }
                if (world.setBlockState(pos, block.getDefaultState(), 2)) {
                    Diagnostics.plantPlaced(plant.key(), pos, biome);
                }
                break;
            }
        }
    }
}