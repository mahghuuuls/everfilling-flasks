package com.mahghuuuls.everfillingflasks.world;

import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Where a ground plant could stand in one column. The search starts at the top of the column and
 * goes down through tree leaves, so a plant can grow on the forest floor under a canopy. The first
 * spot that is air, one snow layer, or one single-block grass or fern, with real ground below, is
 * the answer; the plant replaces the snow or the grass, the way a flower stands in a gap. Anything
 * else on the way (a log, a flower, a double plant, water) ends the search. Whether the ground
 * below suits this plant is the block's call.
 */
final class ColumnFinder {

    /** How far below the column's top the search goes: headroom above the tallest vanilla tree. */
    static final int MAX_DEPTH = 48;

    /** The block at a height of the one column being searched. */
    interface Column {
        IBlockState at(int y);
    }

    /** What one block on the way down is to the search. */
    enum Step {
        /** Leaves: keep going down. */
        PASS,
        /** Air or a spot a plant can take: the answer when the block below is ground, else keep going. */
        OPEN,
        /** Anything else: the column has no spot. */
        BLOCKED
    }

    private ColumnFinder() {
    }

    /** The spot at x and z, or null when the column has none. Reads only that column. */
    static BlockPos find(final World world, final int x, final int z) {
        int top = world.getHeight(new BlockPos(x, 0, z)).getY();
        if (top <= 0 || top >= world.getHeight()) {
            return null;
        }
        Integer y = findY(new Column() {
            @Override
            public IBlockState at(int height) {
                return world.getBlockState(new BlockPos(x, height, z));
            }
        }, top);
        return y == null ? null : new BlockPos(x, y, z);
    }

    /** The height of the spot in a column whose search starts at {@code top}, or null for none. */
    static Integer findY(Column column, int top) {
        for (int y = top, depth = 0; depth < MAX_DEPTH && y > 0; y--, depth++) {
            Step step = stepFor(column.at(y));
            if (step == Step.BLOCKED) {
                return null;
            }
            if (step == Step.OPEN && isGround(column.at(y - 1))) {
                return y;
            }
        }
        return null;
    }

    /** What a block is to the search. */
    static Step stepFor(IBlockState state) {
        Material material = state.getMaterial();
        if (material == Material.AIR) {
            return Step.OPEN;
        }
        if (material == Material.LEAVES) {
            return Step.PASS;
        }
        if (state.getBlock() == Blocks.TALLGRASS
                && state.getValue(BlockTallGrass.TYPE) != BlockTallGrass.EnumType.DEAD_BUSH) {
            return Step.OPEN;
        }
        if (state.getBlock() == Blocks.SNOW_LAYER && state.getValue(BlockSnow.LAYERS) == 1) {
            return Step.OPEN;
        }
        return Step.BLOCKED;
    }

    /** Whether a block can be the ground under a spot: anything but air, leaves, a plant, snow, or a liquid. */
    static boolean isGround(IBlockState state) {
        Material material = state.getMaterial();
        return material != Material.AIR && material != Material.LEAVES && material != Material.PLANTS
                && material != Material.VINE && material != Material.SNOW && !material.isLiquid();
    }
}