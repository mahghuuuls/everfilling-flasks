package com.mahghuuuls.everfillingflasks.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Where a ground plant could stand in one column: the top of the column, when it is air or a
 * single snow layer. The plant replaces that layer and stands on the ground below it, the way a
 * vanilla flower stands in a gap in the snow. Anything else there (tall grass, a flower, thick
 * snow) makes the column unusable. Whether the ground below suits the plant is the block's call.
 */
final class ColumnFinder {

    private ColumnFinder() {
    }

    /** The open spot at the top of the column at x and z, or null when there is none. */
    static BlockPos find(World world, int x, int z) {
        BlockPos top = world.getHeight(new BlockPos(x, 0, z));
        if (top.getY() <= 0 || top.getY() >= world.getHeight()) {
            return null;
        }
        return world.isAirBlock(top) || isThinSnow(world, top) ? top : null;
    }

    private static boolean isThinSnow(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == Blocks.SNOW_LAYER && state.getBlock().isReplaceable(world, pos);
    }
}