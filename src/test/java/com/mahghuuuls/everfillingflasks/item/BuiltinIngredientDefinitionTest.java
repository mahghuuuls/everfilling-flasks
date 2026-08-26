package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.config.FlaskConfig;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Each built-in ingredient's registered cost and contribution, at the config defaults, which
 * are the owner's approved balance numbers (REQ-033). Pure value checks; no item registry or
 * world needed, because the definitions read only the config snapshot.
 */
class BuiltinIngredientDefinitionTest {

    private static FlaskBonuses contributed(IngredientKind kind) {
        FlaskBonuses bonuses = new FlaskBonuses();
        new BuiltinIngredientDefinition(kind).contribute(ItemStack.EMPTY, null, bonuses);
        return bonuses;
    }

    @Test
    void theApprovedCosts() {
        assertEquals(2, new BuiltinIngredientDefinition(IngredientKind.SUNPETAL_LEAF)
                .potencyCost(ItemStack.EMPTY));
        assertEquals(2, new BuiltinIngredientDefinition(IngredientKind.IRONROOT_SPRIG)
                .potencyCost(ItemStack.EMPTY));
        assertEquals(2, new BuiltinIngredientDefinition(IngredientKind.QUICKMINT_LEAF)
                .potencyCost(ItemStack.EMPTY));
        assertEquals(3, new BuiltinIngredientDefinition(IngredientKind.SECOND_WIND_PETAL)
                .potencyCost(ItemStack.EMPTY));
    }

    @Test
    void sunpetalAddsTenPercentHealingAndNothingElse() {
        FlaskBonuses bonuses = contributed(IngredientKind.SUNPETAL_LEAF);
        assertEquals(0.10F, bonuses.healingSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.hitResistanceSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.drinkSpeedSum(), 1.0E-6F);
        assertEquals(0, bonuses.maxChargesFlat());
    }

    @Test
    void ironrootAddsFortyPercentThreshold() {
        FlaskBonuses bonuses = contributed(IngredientKind.IRONROOT_SPRIG);
        assertEquals(0.40F, bonuses.hitResistanceSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.healingSum(), 1.0E-6F);
    }

    @Test
    void quickmintAddsTwentyPercentDrinkSpeed() {
        FlaskBonuses bonuses = contributed(IngredientKind.QUICKMINT_LEAF);
        assertEquals(0.20F, bonuses.drinkSpeedSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.healingSum(), 1.0E-6F);
    }

    @Test
    void thePetalContributesNothingWhilePlaced() {
        // Its whole effect is the post-drink hook; a passive contribution here would be a
        // second, unapproved effect.
        FlaskBonuses bonuses = contributed(IngredientKind.SECOND_WIND_PETAL);
        assertEquals(0.0F, bonuses.healingSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.hitResistanceSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.drinkSpeedSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.rechargeSpeedSum(), 1.0E-6F);
        assertEquals(0, bonuses.maxChargesFlat());
    }

    @Test
    void fiveOfAKindMatchesTheOwnersBalanceIntent() {
        // Five Sunpetal Leaves in a potency-10 Flask: about +50 percent healing.
        FlaskBonuses bonuses = new FlaskBonuses();
        BuiltinIngredientDefinition shard =
                new BuiltinIngredientDefinition(IngredientKind.SUNPETAL_LEAF);
        for (int i = 0; i < 5; i++) {
            shard.contribute(ItemStack.EMPTY, null, bonuses);
        }
        assertEquals(0.50F, bonuses.healingSum(), 1.0E-5F);
    }

    @Test
    void theChestHintIsOfferedOnlyWhileTheHerbsComeFromChests() {
        // A hint must not outlive the behaviour it describes: a pack that takes the herbs out
        // of world loot would otherwise ship a journal sending players to chests that no longer
        // hold them.
        boolean original = FlaskConfig.general.ingredientLoot;
        try {
            FlaskConfig.general.ingredientLoot = true;
            ConfigSnapshot.refresh();
            assertEquals("everfillingflasks.journal.hint.chests",
                    new BuiltinIngredientDefinition(IngredientKind.SUNPETAL_LEAF)
                            .journalHint(ItemStack.EMPTY));

            FlaskConfig.general.ingredientLoot = false;
            ConfigSnapshot.refresh();
            assertNull(new BuiltinIngredientDefinition(IngredientKind.SUNPETAL_LEAF)
                    .journalHint(ItemStack.EMPTY));
        } finally {
            FlaskConfig.general.ingredientLoot = original;
            ConfigSnapshot.refresh();
        }
    }
}
