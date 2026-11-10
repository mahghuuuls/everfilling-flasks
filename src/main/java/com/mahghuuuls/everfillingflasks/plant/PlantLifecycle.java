package com.mahghuuuls.everfillingflasks.plant;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What a pick does, when a stem blooms again, and the light each stage gives off. The block
 * delegates here and decides no stage itself.
 *
 * <p>Picking a bloom drops one item and leaves a stem with a fresh deadline; breaking a stem
 * removes it. Deadlines use the world's total time ({@code getTotalWorldTime}), which runs while
 * the server runs, keeps counting while a chunk is unloaded, and is not moved by the
 * {@code /time} command. A stem past its deadline in an unloaded chunk blooms at its first random
 * tick after the chunk loads.
 */
public final class PlantLifecycle {

    /** Ticks of world time in one minute at the normal tick rate. */
    public static final long TICKS_PER_MINUTE = 1200L;

    /** Minutes a picked plant needs to bloom again at a regrow factor of 1. */
    static final int DEFAULT_REGROW_MINUTES = 180;

    private PlantLifecycle() {
    }

    /** The regrow time in ticks: minutes times 1200 times the factor, rounded to whole ticks. */
    public static long regrowTicks(int baseMinutes, double factor) {
        return Math.round(baseMinutes * (double) TICKS_PER_MINUTE * factor);
    }

    /** The total world time at which a stem picked now blooms again. */
    public static long regrowAt(long now, int baseMinutes, double factor) {
        return now + regrowTicks(baseMinutes, factor);
    }

    /** The light a plant gives off at a stage: its bloom light in bloom, none as a stem. */
    public static int lightFor(WildPlant plant, PlantStage stage) {
        return stage == PlantStage.BLOOM ? plant.bloomLight() : 0;
    }

    /**
     * The stage a plant should show now. Pure so it can be tested without a world: a stem stays
     * a stem until the clock reaches its deadline, and a bloom stays a bloom.
     */
    public static PlantStage decide(PlantStage current, long regrowAt, long now) {
        if (current == PlantStage.STEM && now < regrowAt) {
            return PlantStage.STEM;
        }
        return PlantStage.BLOOM;
    }

    /** Called from the block's random tick on the server. */
    static void tick(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) {
            return;
        }
        PlantStage current = state.getValue(BlockWildPlant.STAGE);
        if (current != PlantStage.STEM) {
            return;
        }
        if (decide(current, deadlineOf(world, pos), world.getTotalWorldTime()) == PlantStage.BLOOM) {
            world.setBlockState(pos, state.withProperty(BlockWildPlant.STAGE, PlantStage.BLOOM), 3);
        }
    }

    /**
     * What breaking the block at this stage does. Returns true when the block should be removed
     * outright (a stem), false when it has been turned into a stem and must stay.
     *
     * <p>On the client the stem is set at once as a prediction, so the held attack key meets a
     * stem (which takes real time to break) instead of breaking again every tick against a bloom
     * the server has already turned into a stem. The server's own block change wins either way.
     *
     * @param drop whether the pick gives an item (false in creative mode)
     */
    static boolean onBroken(World world, BlockPos pos, IBlockState state, WildPlant plant, boolean drop) {
        if (state.getValue(BlockWildPlant.STAGE) == PlantStage.STEM) {
            return true;
        }
        IBlockState stem = state.withProperty(BlockWildPlant.STAGE, PlantStage.STEM);
        if (world.isRemote) {
            world.setBlockState(pos, stem, 11);
            return false;
        }
        Item given = plant.givenItem();
        if (drop && given != null) {
            Block.spawnAsEntity(world, pos, new ItemStack(given));
        }
        world.setBlockState(pos, stem, 3);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityStem) {
            ((TileEntityStem) tile).setRegrowAt(
                    regrowAt(world.getTotalWorldTime(), DEFAULT_REGROW_MINUTES, plant.defaultRegrowFactor()));
        }
        return false;
    }

    private static long deadlineOf(World world, BlockPos pos) {
        // A stem saved without its tile entity gets a fresh one from the world here, with a
        // deadline of 0, so it blooms at once rather than never.
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityStem ? ((TileEntityStem) tile).regrowAt() : Long.MIN_VALUE;
    }
}
