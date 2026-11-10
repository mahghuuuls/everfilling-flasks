package com.mahghuuuls.everfillingflasks.world;

import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What each block is to the downward spot search, and what counts as ground below a spot. */
class ColumnFinderTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
        AIR = Blocks.AIR.getDefaultState();
        LEAVES = Blocks.LEAVES.getDefaultState();
        GRASS = Blocks.GRASS.getDefaultState();
        TALL = Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
    }

    @Test
    void leavesAreSearchedThroughAndAirIsASpot() {
        assertEquals(ColumnFinder.Step.OPEN, ColumnFinder.stepFor(Blocks.AIR.getDefaultState()));
        assertEquals(ColumnFinder.Step.PASS, ColumnFinder.stepFor(Blocks.LEAVES.getDefaultState()));
        assertEquals(ColumnFinder.Step.PASS, ColumnFinder.stepFor(Blocks.LEAVES2.getDefaultState()));
    }

    @Test
    void aShrubIsNotReplaced() {
        assertEquals(ColumnFinder.Step.BLOCKED, ColumnFinder.stepFor(Blocks.TALLGRASS.getDefaultState()
                .withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.DEAD_BUSH)));
    }

    /** A column from the top down: index 0 is y = top. Below the listed blocks, stone. */
    private static ColumnFinder.Column column(final int top, final IBlockState... fromTop) {
        return new ColumnFinder.Column() {
            @Override
            public IBlockState at(int y) {
                int i = top - y;
                return i >= 0 && i < fromTop.length ? fromTop[i] : Blocks.STONE.getDefaultState();
            }
        };
    }

    // Filled after the bootstrap: reading Blocks before it breaks the block list for every test.
    private static IBlockState AIR;
    private static IBlockState LEAVES;
    private static IBlockState GRASS;
    private static IBlockState TALL;

    @Test
    void underACanopyTheSpotIsTheTallGrassOnTheFloor() {
        // y 100: air, 99 to 97: leaves, 96: air, 95: tall grass, 94: grass.
        assertEquals(Integer.valueOf(95), ColumnFinder.findY(column(100, AIR, LEAVES, LEAVES, LEAVES, AIR, TALL, GRASS), 100));
    }

    @Test
    void onOpenGroundTheSpotIsTheTop() {
        assertEquals(Integer.valueOf(70), ColumnFinder.findY(column(70, AIR, GRASS), 70));
    }

    @Test
    void snowOnLeavesIsPassedToTheFloor() {
        IBlockState snow = Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, 1);
        assertEquals(Integer.valueOf(66), ColumnFinder.findY(column(70, snow, LEAVES, LEAVES, AIR, AIR, GRASS), 70));
    }

    @Test
    void leavesRightOnTheGroundLeaveNoSpot() {
        assertEquals(null, ColumnFinder.findY(column(70, LEAVES, GRASS), 70));
    }

    @Test
    void theSearchNeverGoesBelowALogOrWater() {
        // Air over a log is a spot; whether a log suits a plant is the plant's ground rule, which
        // refuses it. The search does not look under the log for the grass there.
        assertEquals(Integer.valueOf(70), ColumnFinder.findY(column(70, AIR, Blocks.LOG.getDefaultState(), AIR, GRASS), 70));
        assertEquals(null, ColumnFinder.findY(column(70, AIR, Blocks.WATER.getDefaultState(), GRASS), 70));
    }

    @Test
    void theSearchStopsAfterItsDepth() {
        IBlockState[] tall = new IBlockState[ColumnFinder.MAX_DEPTH + 2];
        for (int i = 0; i < tall.length - 1; i++) {
            tall[i] = LEAVES;
        }
        tall[tall.length - 1] = GRASS;
        assertEquals(null, ColumnFinder.findY(column(200, tall), 200));
    }

    @Test
    void theSearchNeverReadsBelowTheWorld() {
        final int[] lowest = {Integer.MAX_VALUE};
        ColumnFinder.Column leavesToTheBottom = new ColumnFinder.Column() {
            @Override
            public IBlockState at(int y) {
                lowest[0] = Math.min(lowest[0], y);
                return LEAVES;
            }
        };
        assertEquals(null, ColumnFinder.findY(leavesToTheBottom, 10));
        assertTrue(lowest[0] >= 0);
    }

    @Test
    void tallGrassFernsAndOneSnowLayerAreSpots() {
        assertEquals(ColumnFinder.Step.OPEN, ColumnFinder.stepFor(Blocks.TALLGRASS.getDefaultState()
                .withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS)));
        assertEquals(ColumnFinder.Step.OPEN, ColumnFinder.stepFor(Blocks.TALLGRASS.getDefaultState()
                .withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.FERN)));
        assertEquals(ColumnFinder.Step.OPEN, ColumnFinder.stepFor(
                Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, 1)));
    }

    @Test
    void thickSnowFlowersDoublePlantsLogsAndWaterEndTheSearch() {
        assertEquals(ColumnFinder.Step.BLOCKED, ColumnFinder.stepFor(
                Blocks.SNOW_LAYER.getDefaultState().withProperty(BlockSnow.LAYERS, 2)));
        assertEquals(ColumnFinder.Step.BLOCKED, ColumnFinder.stepFor(Blocks.RED_FLOWER.getDefaultState()));
        assertEquals(ColumnFinder.Step.BLOCKED, ColumnFinder.stepFor(Blocks.DOUBLE_PLANT.getDefaultState()));
        assertEquals(ColumnFinder.Step.BLOCKED, ColumnFinder.stepFor(Blocks.LOG.getDefaultState()));
        assertEquals(ColumnFinder.Step.BLOCKED, ColumnFinder.stepFor(Blocks.WATER.getDefaultState()));
        assertEquals(ColumnFinder.Step.BLOCKED, ColumnFinder.stepFor(Blocks.GRASS.getDefaultState()));
    }

    @Test
    void groundIsASolidBlockNotFoliageOrWater() {
        assertTrue(ColumnFinder.isGround(Blocks.GRASS.getDefaultState()));
        assertTrue(ColumnFinder.isGround(Blocks.DIRT.getDefaultState()));
        assertTrue(ColumnFinder.isGround(Blocks.STONE.getDefaultState()));
        assertFalse(ColumnFinder.isGround(Blocks.LEAVES.getDefaultState()));
        assertFalse(ColumnFinder.isGround(Blocks.AIR.getDefaultState()));
        assertFalse(ColumnFinder.isGround(Blocks.TALLGRASS.getDefaultState()));
        assertFalse(ColumnFinder.isGround(Blocks.WATER.getDefaultState()));
        assertFalse(ColumnFinder.isGround(Blocks.SNOW_LAYER.getDefaultState()));
    }
}