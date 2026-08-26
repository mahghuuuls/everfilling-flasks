package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The infusion engine: cost summing, refusals, isolation, and the one-accumulator merge
 * order that keeps infusion and player bonuses adding before any base is multiplied.
 */
class InfusionRegistryTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @org.junit.jupiter.api.BeforeEach
    void clearBefore() {
        // Before as well as after: another test class may have registered the same items into
        // the shared static registry, and first-wins would silently swap this class's fixtures.
        InfusionRegistry.clearForTests();
    }

    @AfterEach
    void clearRegistry() {
        InfusionRegistry.clearForTests();
    }

    private static final class Fixed implements InfusionDefinition {
        private final int cost;
        private final float healing;

        Fixed(int cost, float healing) {
            this.cost = cost;
            this.healing = healing;
        }

        @Override
        public int potencyCost(ItemStack infusion) {
            return cost;
        }

        @Override
        public void contribute(ItemStack infusion, EntityPlayer player, FlaskBonuses bonuses) {
            bonuses.healing(healing);
        }
    }

    @Test
    void costsSumAcrossPlacedPiecesAndUnregisteredPiecesAreFree() {
        InfusionRegistry.register(Items.SUGAR, new Fixed(2, 0.1F));
        NonNullList<ItemStack> grid = NonNullList.withSize(FlaskStackState.GRID_SIZE, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));
        grid.set(4, new ItemStack(Items.SUGAR));
        grid.set(5, new ItemStack(Items.SUGAR));
        // Not registered: contributes nothing, costs nothing.
        grid.set(2, new ItemStack(Items.APPLE));

        assertEquals(6, InfusionRegistry.usedPotency(grid));
    }

    @Test
    void negativeCostsAreFlooredAtZero() {
        InfusionRegistry.register(Items.SUGAR, new Fixed(-5, 0.0F));
        NonNullList<ItemStack> grid = NonNullList.withSize(FlaskStackState.GRID_SIZE, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));

        assertEquals(0, InfusionRegistry.usedPotency(grid));
    }

    @Test
    void infusionAndPlayerBonusesShareOneAccumulator() {
        InfusionRegistry.register(Items.SUGAR, new Fixed(2, 0.25F));
        NonNullList<ItemStack> grid = NonNullList.withSize(FlaskStackState.GRID_SIZE, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));

        // A player modifier already contributed 0.5; the infusion's 0.25 must ADD to it.
        FlaskBonuses bonuses = new FlaskBonuses();
        bonuses.healing(0.5F);
        InfusionRegistry.contribute(grid, null, bonuses);
        EffectiveFlask effective =
                FlaskMechanics.effective(2, 0.30F, 1200, 30, 1.0F, bonuses);

        // 0.30 * (1 + 0.5 + 0.25) = 0.525. The wrong model, multiplying multipliers,
        // would give 0.30 * 1.5 * 1.25 = 0.5625.
        assertEquals(0.525F, effective.healPercentage(), 1.0E-5F);
    }

    @Test
    void duplicateAndNullRegistrationsAreRefused() {
        assertTrue(InfusionRegistry.register(Items.SUGAR, new Fixed(1, 0.0F)));
        assertFalse(InfusionRegistry.register(Items.SUGAR, new Fixed(9, 0.0F)),
                "the first registration keeps the item");
        assertFalse(InfusionRegistry.register(null, new Fixed(1, 0.0F)));
        assertFalse(InfusionRegistry.register(Items.APPLE, null));

        NonNullList<ItemStack> grid = NonNullList.withSize(FlaskStackState.GRID_SIZE, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));
        assertEquals(1, InfusionRegistry.usedPotency(grid), "the first definition's cost");
        assertTrue(InfusionRegistry.isInfusion(new ItemStack(Items.SUGAR)));
        assertFalse(InfusionRegistry.isInfusion(new ItemStack(Items.APPLE)));
    }

    @Test
    void aThrowingDefinitionIsSkippedAndTheOthersApply() {
        InfusionRegistry.register(Items.SUGAR, new InfusionDefinition() {
            @Override
            public int potencyCost(ItemStack infusion) {
                throw new IllegalStateException("test fixture: always fails");
            }

            @Override
            public void contribute(ItemStack infusion, EntityPlayer player,
                                   FlaskBonuses bonuses) {
                throw new IllegalStateException("test fixture: always fails");
            }
        });
        InfusionRegistry.register(Items.APPLE, new Fixed(3, 0.2F));
        NonNullList<ItemStack> grid = NonNullList.withSize(FlaskStackState.GRID_SIZE, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));
        grid.set(1, new ItemStack(Items.APPLE));

        assertEquals(3, InfusionRegistry.usedPotency(grid),
                "the throwing piece is skipped, the healthy one still counts");
        FlaskBonuses bonuses = new FlaskBonuses();
        InfusionRegistry.contribute(grid, null, bonuses);
        assertEquals(0.2F, bonuses.healingSum(), 1.0E-6F,
                "the healthy piece's contribution survives its neighbor's failure");
    }
}
