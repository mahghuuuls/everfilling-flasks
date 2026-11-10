package com.mahghuuuls.everfillingflasks.plant;

import com.mahghuuuls.everfillingflasks.item.InfusionKind;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The five plants: their registry paths, the infusion each gives (none for the seed plant), and
 * what each may stand on. Which item object a plant drops needs the
 * registered items and is checked in game.
 */
class WildPlantTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void theRegistryPathsAreTheApprovedOnes() {
        assertEquals("sunpetal", WildPlant.SUNPETAL.key());
        assertEquals("ironroot", WildPlant.IRONROOT.key());
        assertEquals("quickmint", WildPlant.QUICKMINT.key());
        assertEquals("second_wind", WildPlant.SECOND_WIND.key());
        assertEquals("everlasting_sprout", WildPlant.EVERLASTING_SPROUT.key());
    }

    @Test
    void eachPlantHasItsApprovedInfusionAndGround() {
        assertPlant(WildPlant.SUNPETAL, InfusionKind.SUNPETAL_LEAF, PlantGround.GRASS);
        assertPlant(WildPlant.IRONROOT, InfusionKind.IRONROOT_SPRIG, PlantGround.GRASS_OR_STONE);
        assertPlant(WildPlant.QUICKMINT, InfusionKind.QUICKMINT_LEAF, PlantGround.GRASS_OR_DIRT);
        assertPlant(WildPlant.SECOND_WIND, InfusionKind.SECOND_WIND_PETAL, PlantGround.GRASS);
        assertPlant(WildPlant.EVERLASTING_SPROUT, null, PlantGround.GRASS_OR_DIRT);
    }

    private static void assertPlant(WildPlant plant, InfusionKind infusion, PlantGround ground) {
        assertEquals(infusion, plant.infusion(), plant + " infusion");
        assertEquals(ground, plant.ground(), plant + " ground");
    }

    @Test
    void everyBuiltInInfusionHasExactlyOnePlantAndOnePlantGivesTheSeed() {
        Set<InfusionKind> given = EnumSet.noneOf(InfusionKind.class);
        int seedPlants = 0;
        for (WildPlant plant : WildPlant.values()) {
            if (plant.infusion() == null) {
                seedPlants++;
            } else {
                assertTrue(given.add(plant.infusion()), plant + " repeats an infusion");
            }
        }
        assertEquals(EnumSet.allOf(InfusionKind.class), given);
        assertEquals(1, seedPlants);
        assertNull(WildPlant.EVERLASTING_SPROUT.infusion());
    }

    @Test
    void grassAcceptsOnlyGrass() {
        PlantGround ground = PlantGround.GRASS;
        assertTrue(ground.accepts(Blocks.GRASS.getDefaultState()));
        assertFalse(ground.accepts(Blocks.DIRT.getDefaultState()));
        assertFalse(ground.accepts(Blocks.STONE.getDefaultState()));
        assertFalse(ground.accepts(Blocks.SAND.getDefaultState()));
    }

    @Test
    void grassOrDirtAcceptsEveryDirtVariant() {
        PlantGround ground = PlantGround.GRASS_OR_DIRT;
        assertTrue(ground.accepts(Blocks.GRASS.getDefaultState()));
        assertTrue(ground.accepts(Blocks.DIRT.getDefaultState()));
        assertTrue(ground.accepts(Blocks.DIRT.getStateFromMeta(1)));
        assertTrue(ground.accepts(Blocks.DIRT.getStateFromMeta(2)));
        assertFalse(ground.accepts(Blocks.STONE.getDefaultState()));
        assertFalse(ground.accepts(Blocks.MYCELIUM.getDefaultState()));
    }

    @Test
    void grassOrStoneAcceptsNaturalRock() {
        PlantGround ground = PlantGround.GRASS_OR_STONE;
        assertTrue(ground.accepts(Blocks.GRASS.getDefaultState()));
        assertTrue(ground.accepts(Blocks.STONE.getDefaultState()));
        assertTrue(ground.accepts(Blocks.STONE.getStateFromMeta(1)));
        assertFalse(ground.accepts(Blocks.DIRT.getDefaultState()));
        assertFalse(ground.accepts(Blocks.COBBLESTONE.getDefaultState()));
        assertFalse(ground.accepts(Blocks.STONEBRICK.getDefaultState()));
        assertFalse(ground.accepts(Blocks.LOG.getDefaultState()));
    }
}
