package com.mahghuuuls.everfillingflasks.plant;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

/**
 * What a wild plant may stand on. Dirt includes coarse dirt and podzol, and stone includes
 * granite, diorite, and andesite: each is a variant of one vanilla block. Placed stonework
 * (cobblestone, bricks, slabs) is not natural ground and does not count.
 */
public enum PlantGround {

    GRASS(true, false, false),
    GRASS_OR_DIRT(true, true, false),
    GRASS_OR_STONE(true, false, true);

    private final boolean grass;
    private final boolean dirt;
    private final boolean stone;

    PlantGround(boolean grass, boolean dirt, boolean stone) {
        this.grass = grass;
        this.dirt = dirt;
        this.stone = stone;
    }

    /** Polished granite, diorite, and andesite are stonework, not natural ground. */
    private static boolean isPolished(IBlockState stone) {
        BlockStone.EnumType type = stone.getValue(BlockStone.VARIANT);
        return type == BlockStone.EnumType.GRANITE_SMOOTH || type == BlockStone.EnumType.DIORITE_SMOOTH
                || type == BlockStone.EnumType.ANDESITE_SMOOTH;
    }

    /** Whether a plant of this kind may stand on the given block. */
    public boolean accepts(IBlockState below) {
        Block block = below.getBlock();
        if (grass && block == Blocks.GRASS) {
            return true;
        }
        if (dirt && block == Blocks.DIRT) {
            return true;
        }
        return stone && block == Blocks.STONE && !isPolished(below);
    }
}
