package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The conflict rules (REQ-051): either side declaring is enough, the grid walks find the right
 * piece, a clean grid is untouched, and a throwing check counts as no conflict.
 */
class InfusionConflictTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @BeforeEach
    void clearBefore() {
        InfusionRegistry.clearForTests();
    }

    @AfterEach
    void clearAfter() {
        InfusionRegistry.clearForTests();
    }

    /** Costs 1 and conflicts with exactly one item, or with nothing when that item is null. */
    private static final class Picky implements InfusionDefinition {
        private final Item refuses;

        Picky(Item refuses) {
            this.refuses = refuses;
        }

        @Override
        public int potencyCost(ItemStack infusion) {
            return 1;
        }

        @Override
        public boolean conflictsWith(ItemStack self, ItemStack other) {
            return refuses != null && other.getItem() == refuses;
        }
    }

    private static NonNullList<ItemStack> grid(Item... items) {
        NonNullList<ItemStack> grid = NonNullList.withSize(5, ItemStack.EMPTY);
        for (int i = 0; i < items.length; i++) {
            grid.set(i, new ItemStack(items[i]));
        }
        return grid;
    }

    @Test
    void eitherSideDeclaringIsEnough() {
        InfusionRegistry.register(Items.CLAY_BALL, new Picky(Items.BRICK));
        InfusionRegistry.register(Items.BRICK, new Picky(null));
        ItemStack clay = new ItemStack(Items.CLAY_BALL);
        ItemStack brick = new ItemStack(Items.BRICK);

        assertTrue(InfusionRegistry.conflicts(clay, brick));
        // The brick says nothing, yet the pair still conflicts from the clay's side.
        assertTrue(InfusionRegistry.conflicts(brick, clay));
    }

    @Test
    void piecesThatDeclareNothingNeverConflict() {
        InfusionRegistry.register(Items.CLAY_BALL, new Picky(null));
        InfusionRegistry.register(Items.BRICK, new Picky(null));

        assertFalse(InfusionRegistry.conflicts(new ItemStack(Items.CLAY_BALL),
                new ItemStack(Items.BRICK)));
        assertFalse(InfusionRegistry.hasConflict(grid(Items.CLAY_BALL, Items.BRICK)));
    }

    @Test
    void firstConflictNamesThePlacedPiece() {
        InfusionRegistry.register(Items.CLAY_BALL, new Picky(null));
        InfusionRegistry.register(Items.BRICK, new Picky(null));
        InfusionRegistry.register(Items.FLINT, new Picky(Items.BRICK));

        ItemStack clash = InfusionRegistry.firstConflict(grid(Items.CLAY_BALL, Items.BRICK),
                new ItemStack(Items.FLINT));

        assertEquals(Items.BRICK, clash.getItem());
        assertTrue(InfusionRegistry.firstConflict(grid(Items.CLAY_BALL),
                new ItemStack(Items.FLINT)).isEmpty());
    }

    @Test
    void anIdenticalPairConflictsOnlyWhenTheDefinitionSaysSo() {
        InfusionRegistry.register(Items.CLAY_BALL, new Picky(Items.CLAY_BALL));
        InfusionRegistry.register(Items.BRICK, new Picky(null));

        assertTrue(InfusionRegistry.hasConflict(grid(Items.CLAY_BALL, Items.CLAY_BALL)));
        assertFalse(InfusionRegistry.hasConflict(grid(Items.BRICK, Items.BRICK)));
    }

    @Test
    void hasConflictFindsAPairAnywhereInTheGrid() {
        InfusionRegistry.register(Items.CLAY_BALL, new Picky(null));
        InfusionRegistry.register(Items.BRICK, new Picky(null));
        InfusionRegistry.register(Items.FLINT, new Picky(Items.CLAY_BALL));
        NonNullList<ItemStack> grid = NonNullList.withSize(12, ItemStack.EMPTY);
        grid.set(1, new ItemStack(Items.CLAY_BALL));
        grid.set(6, new ItemStack(Items.BRICK));
        grid.set(11, new ItemStack(Items.FLINT));

        assertTrue(InfusionRegistry.hasConflict(grid));
    }

    @Test
    void aThrowingCheckCountsAsNoConflict() {
        InfusionRegistry.register(Items.CLAY_BALL, new InfusionDefinition() {
            @Override
            public int potencyCost(ItemStack infusion) {
                return 1;
            }

            @Override
            public boolean conflictsWith(ItemStack self, ItemStack other) {
                throw new IllegalStateException("test check that always fails");
            }
        });
        InfusionRegistry.register(Items.BRICK, new Picky(null));

        // Would lock the grid if a throw counted as a conflict.
        assertFalse(InfusionRegistry.hasConflict(grid(Items.CLAY_BALL, Items.BRICK)));
    }
}
