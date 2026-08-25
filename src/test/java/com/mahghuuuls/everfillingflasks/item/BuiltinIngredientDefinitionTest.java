package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(2, new BuiltinIngredientDefinition(IngredientKind.SUNMELON_SHARD)
                .potencyCost(ItemStack.EMPTY));
        assertEquals(2, new BuiltinIngredientDefinition(IngredientKind.IRONBARK_CHIP)
                .potencyCost(ItemStack.EMPTY));
        assertEquals(2, new BuiltinIngredientDefinition(IngredientKind.QUICKSILVER_DROP)
                .potencyCost(ItemStack.EMPTY));
        assertEquals(3, new BuiltinIngredientDefinition(IngredientKind.SECOND_WIND_PETAL)
                .potencyCost(ItemStack.EMPTY));
    }

    @Test
    void sunmelonAddsTenPercentHealingAndNothingElse() {
        FlaskBonuses bonuses = contributed(IngredientKind.SUNMELON_SHARD);
        assertEquals(0.10F, bonuses.healingSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.hitResistanceSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.drinkSpeedSum(), 1.0E-6F);
        assertEquals(0, bonuses.maxChargesFlat());
    }

    @Test
    void ironbarkAddsFortyPercentThreshold() {
        FlaskBonuses bonuses = contributed(IngredientKind.IRONBARK_CHIP);
        assertEquals(0.40F, bonuses.hitResistanceSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.healingSum(), 1.0E-6F);
    }

    @Test
    void quicksilverAddsTwentyPercentDrinkSpeed() {
        FlaskBonuses bonuses = contributed(IngredientKind.QUICKSILVER_DROP);
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
        // Five Sunmelon Shards in a potency-10 Flask: about +50 percent healing.
        FlaskBonuses bonuses = new FlaskBonuses();
        BuiltinIngredientDefinition shard =
                new BuiltinIngredientDefinition(IngredientKind.SUNMELON_SHARD);
        for (int i = 0; i < 5; i++) {
            shard.contribute(ItemStack.EMPTY, null, bonuses);
        }
        assertEquals(0.50F, bonuses.healingSum(), 1.0E-5F);
    }
}
