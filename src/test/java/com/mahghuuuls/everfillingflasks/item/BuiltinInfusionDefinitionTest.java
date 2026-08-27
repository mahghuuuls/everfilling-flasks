package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.config.FlaskConfig;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Each built-in infusion's registered cost and contribution, at the config defaults, which
 * are the owner's approved balance numbers (REQ-033). Pure value checks; no item registry or
 * world needed, because the definitions read only the config snapshot.
 */
class BuiltinInfusionDefinitionTest {

    private static FlaskBonuses contributed(InfusionKind kind) {
        FlaskBonuses bonuses = new FlaskBonuses();
        new BuiltinInfusionDefinition(kind).contribute(ItemStack.EMPTY, null, bonuses);
        return bonuses;
    }

    @Test
    void theApprovedCosts() {
        assertEquals(2, new BuiltinInfusionDefinition(InfusionKind.SUNPETAL_LEAF)
                .potencyCost(ItemStack.EMPTY));
        assertEquals(2, new BuiltinInfusionDefinition(InfusionKind.IRONROOT_SPRIG)
                .potencyCost(ItemStack.EMPTY));
        assertEquals(2, new BuiltinInfusionDefinition(InfusionKind.QUICKMINT_LEAF)
                .potencyCost(ItemStack.EMPTY));
        assertEquals(3, new BuiltinInfusionDefinition(InfusionKind.SECOND_WIND_PETAL)
                .potencyCost(ItemStack.EMPTY));
    }

    @Test
    void sunpetalAddsEightPercentHealingAndNothingElse() {
        FlaskBonuses bonuses = contributed(InfusionKind.SUNPETAL_LEAF);
        assertEquals(0.08F, bonuses.healingSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.hitResistanceSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.drinkSpeedSum(), 1.0E-6F);
        assertEquals(0, bonuses.maxChargesFlat());
    }

    @Test
    void ironrootAddsThirtyTwoPercentThreshold() {
        FlaskBonuses bonuses = contributed(InfusionKind.IRONROOT_SPRIG);
        assertEquals(0.32F, bonuses.hitResistanceSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.healingSum(), 1.0E-6F);
    }

    @Test
    void quickmintAddsSixteenPercentDrinkSpeed() {
        FlaskBonuses bonuses = contributed(InfusionKind.QUICKMINT_LEAF);
        assertEquals(0.16F, bonuses.drinkSpeedSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.healingSum(), 1.0E-6F);
    }

    @Test
    void thePetalContributesNothingWhilePlaced() {
        // Its whole effect is the post-drink hook; a passive contribution here would be a
        // second, unapproved effect.
        FlaskBonuses bonuses = contributed(InfusionKind.SECOND_WIND_PETAL);
        assertEquals(0.0F, bonuses.healingSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.hitResistanceSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.drinkSpeedSum(), 1.0E-6F);
        assertEquals(0.0F, bonuses.rechargeSpeedSum(), 1.0E-6F);
        assertEquals(0, bonuses.maxChargesFlat());
    }

    @Test
    void aFullGridOfOneKindMatchesTheOwnersBalanceIntent() {
        // Five Sunpetal Leaves fill a potency-10 Flask exactly: about +40 percent healing.
        FlaskBonuses bonuses = new FlaskBonuses();
        BuiltinInfusionDefinition shard =
                new BuiltinInfusionDefinition(InfusionKind.SUNPETAL_LEAF);
        for (int i = 0; i < 5; i++) {
            shard.contribute(ItemStack.EMPTY, null, bonuses);
        }
        assertEquals(0.40F, bonuses.healingSum(), 1.0E-5F);
    }

    @Test
    void theChestHintIsOfferedOnlyWhileTheHerbsComeFromChests() {
        // A hint must not outlive the behaviour it describes: a pack that takes the herbs out
        // of world loot would otherwise ship a journal sending players to chests that no longer
        // hold them.
        boolean original = FlaskConfig.general.infusionLoot;
        try {
            FlaskConfig.general.infusionLoot = true;
            ConfigSnapshot.refresh();
            assertEquals("everfillingflasks.journal.text.chests",
                    new BuiltinInfusionDefinition(InfusionKind.SUNPETAL_LEAF)
                            .journalText(ItemStack.EMPTY));

            FlaskConfig.general.infusionLoot = false;
            ConfigSnapshot.refresh();
            assertNull(new BuiltinInfusionDefinition(InfusionKind.SUNPETAL_LEAF)
                    .journalText(ItemStack.EMPTY));
        } finally {
            FlaskConfig.general.infusionLoot = original;
            ConfigSnapshot.refresh();
        }
    }
}
