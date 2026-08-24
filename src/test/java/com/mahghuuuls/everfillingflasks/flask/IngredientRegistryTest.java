package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.IngredientDefinition;
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
 * The ingredient engine: cost summing, refusals, isolation, and the one-accumulator merge
 * order that keeps ingredient and player bonuses adding before any base is multiplied.
 */
class IngredientRegistryTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @AfterEach
    void clearRegistry() {
        IngredientRegistry.clearForTests();
    }

    private static final class Fixed implements IngredientDefinition {
        private final int cost;
        private final float healing;

        Fixed(int cost, float healing) {
            this.cost = cost;
            this.healing = healing;
        }

        @Override
        public int potencyCost(ItemStack ingredient) {
            return cost;
        }

        @Override
        public void contribute(ItemStack ingredient, EntityPlayer player, FlaskBonuses bonuses) {
            bonuses.healing(healing);
        }
    }

    @Test
    void costsSumAcrossPlacedPiecesAndUnregisteredPiecesAreFree() {
        IngredientRegistry.register(Items.SUGAR, new Fixed(2, 0.1F));
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));
        grid.set(4, new ItemStack(Items.SUGAR));
        grid.set(8, new ItemStack(Items.SUGAR));
        // Not registered: contributes nothing, costs nothing.
        grid.set(2, new ItemStack(Items.APPLE));

        assertEquals(6, IngredientRegistry.usedPotency(grid));
    }

    @Test
    void negativeCostsAreFlooredAtZero() {
        IngredientRegistry.register(Items.SUGAR, new Fixed(-5, 0.0F));
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));

        assertEquals(0, IngredientRegistry.usedPotency(grid));
    }

    @Test
    void ingredientAndPlayerBonusesShareOneAccumulator() {
        IngredientRegistry.register(Items.SUGAR, new Fixed(2, 0.25F));
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));

        // A player modifier already contributed 0.5; the ingredient's 0.25 must ADD to it.
        FlaskBonuses bonuses = new FlaskBonuses();
        bonuses.healing(0.5F);
        IngredientRegistry.contribute(grid, null, bonuses);
        EffectiveFlask effective =
                FlaskMechanics.effective(2, 0.30F, 1200, 30, 1.0F, bonuses);

        // 0.30 * (1 + 0.5 + 0.25) = 0.525. The wrong model, multiplying multipliers,
        // would give 0.30 * 1.5 * 1.25 = 0.5625.
        assertEquals(0.525F, effective.healPercentage(), 1.0E-5F);
    }

    @Test
    void duplicateAndNullRegistrationsAreRefused() {
        assertTrue(IngredientRegistry.register(Items.SUGAR, new Fixed(1, 0.0F)));
        assertFalse(IngredientRegistry.register(Items.SUGAR, new Fixed(9, 0.0F)),
                "the first registration keeps the item");
        assertFalse(IngredientRegistry.register(null, new Fixed(1, 0.0F)));
        assertFalse(IngredientRegistry.register(Items.APPLE, null));

        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));
        assertEquals(1, IngredientRegistry.usedPotency(grid), "the first definition's cost");
        assertTrue(IngredientRegistry.isIngredient(new ItemStack(Items.SUGAR)));
        assertFalse(IngredientRegistry.isIngredient(new ItemStack(Items.APPLE)));
    }

    @Test
    void aThrowingDefinitionIsSkippedAndTheOthersApply() {
        IngredientRegistry.register(Items.SUGAR, new IngredientDefinition() {
            @Override
            public int potencyCost(ItemStack ingredient) {
                throw new IllegalStateException("test fixture: always fails");
            }

            @Override
            public void contribute(ItemStack ingredient, EntityPlayer player,
                                   FlaskBonuses bonuses) {
                throw new IllegalStateException("test fixture: always fails");
            }
        });
        IngredientRegistry.register(Items.APPLE, new Fixed(3, 0.2F));
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        grid.set(0, new ItemStack(Items.SUGAR));
        grid.set(1, new ItemStack(Items.APPLE));

        assertEquals(3, IngredientRegistry.usedPotency(grid),
                "the throwing piece is skipped, the healthy one still counts");
        FlaskBonuses bonuses = new FlaskBonuses();
        IngredientRegistry.contribute(grid, null, bonuses);
        assertEquals(0.2F, bonuses.healingSum(), 1.0E-6F,
                "the healthy piece's contribution survives its neighbor's failure");
    }
}
