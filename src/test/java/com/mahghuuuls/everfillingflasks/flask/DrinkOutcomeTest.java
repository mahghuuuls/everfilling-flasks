package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.DrinkOutcome;
import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The 1.1.0 completion contract: the outcome form is what the core calls, a 1.0.0 definition is
 * still called exactly once through it, the applied heal is a floored difference, and effect
 * power sums like the other channels and floors at zero.
 */
class DrinkOutcomeTest {

    /** A definition written against 1.0.0: overrides only the plain hook. */
    private static final class PlainHookDefinition implements FlaskDefinition {
        int plainCalls;
        ItemStack lastStack;

        @Override public int maxCharges(ItemStack stack, EntityPlayer player) { return 1; }
        @Override public float healPercentage(ItemStack stack, EntityPlayer player) { return 0.3F; }
        @Override public int rechargeTicks(ItemStack stack, EntityPlayer player) { return 600; }
        @Override public int drinkTicks(ItemStack stack, EntityPlayer player) { return 30; }
        @Override public float hitThreshold(ItemStack stack, EntityPlayer player) { return 1.0F; }

        @Override
        public void onDrinkCompleted(ItemStack stack, EntityPlayer player) {
            plainCalls++;
            lastStack = stack;
        }
    }

    @Test
    void theOutcomeFormForwardsToThePlainHookExactlyOnce() {
        PlainHookDefinition definition = new PlainHookDefinition();
        ItemStack stack = ItemStack.EMPTY;
        DrinkOutcome outcome = new DrinkOutcome(10.0F, 6.0F, 6.0F, 0.0F, 0, 1.0F);

        definition.onDrinkCompleted(stack, null, outcome);

        // Would be 0 if the default did not forward. That the core calls only the outcome form,
        // and so never reaches a definition twice, is enforced in DrinkController.
        assertEquals(1, definition.plainCalls);
        assertSame(stack, definition.lastStack);
    }

    @Test
    void theOutcomeCarriesWhatItWasGiven() {
        DrinkOutcome outcome = new DrinkOutcome(18.0F, 6.0F, 2.0F, 4.4F, 2, 1.5F);
        assertEquals(18.0F, outcome.healthBefore(), 1.0E-6F);
        assertEquals(6.0F, outcome.healRequested(), 1.0E-6F);
        assertEquals(2.0F, outcome.healApplied(), 1.0E-6F);
        assertEquals(4.4F, outcome.healOverTimeScheduled(), 1.0E-6F);
        assertEquals(2, outcome.chargesLeft());
        assertEquals(1.5F, outcome.effectPower(), 1.0E-6F);
    }

    @Test
    void appliedHealIsTheDifferenceFlooredAtZero() {
        // Half health, full heal landed.
        assertEquals(6.0F, FlaskMechanics.healApplied(10.0F, 16.0F), 1.0E-6F);
        // Near full: only the room left was healed.
        assertEquals(2.0F, FlaskMechanics.healApplied(18.0F, 20.0F), 1.0E-6F);
        // Full health: nothing happened.
        assertEquals(0.0F, FlaskMechanics.healApplied(20.0F, 20.0F), 1.0E-6F);
        // Something lowered health between the reads: reported as 0, never negative.
        assertEquals(0.0F, FlaskMechanics.healApplied(20.0F, 19.0F), 1.0E-6F);
    }

    @Test
    void effectPowerIsOnePlusTheSum() {
        FlaskBonuses b = new FlaskBonuses();
        b.effectPower(0.5F);
        b.effectPower(0.25F);
        assertEquals(1.75F,
                FlaskMechanics.effective(2, 0.3F, 600, 30, 1.0F, b).effectPower(), 1.0E-6F);
    }

    @Test
    void effectPowerIsOneWithNoSourceAndFloorsAtZero() {
        assertEquals(1.0F,
                FlaskMechanics.effective(2, 0.3F, 600, 30, 1.0F, new FlaskBonuses())
                        .effectPower(), 1.0E-6F);
        FlaskBonuses minus = new FlaskBonuses();
        minus.effectPower(-2.0F);
        // A multiplier below zero would flip an effect; the floor keeps it at "nothing".
        assertEquals(0.0F,
                FlaskMechanics.effective(2, 0.3F, 600, 30, 1.0F, minus).effectPower(), 1.0E-6F);
    }

    @Test
    void effectPowerDoesNotTouchHealing() {
        FlaskBonuses b = new FlaskBonuses();
        b.effectPower(1.0F);
        EffectiveFlask e = FlaskMechanics.effective(2, 0.3F, 600, 30, 1.0F, b);
        // Would be 0.6 if the channels were confused.
        assertEquals(0.3F, e.healPercentage(), 1.0E-6F);
    }

    @Test
    void healingDoesNotTouchEffectPower() {
        FlaskBonuses b = new FlaskBonuses();
        b.healing(1.0F);
        // The reverse confusion: a healing bonus must leave effect power at its default.
        assertEquals(1.0F,
                FlaskMechanics.effective(2, 0.3F, 600, 30, 1.0F, b).effectPower(), 1.0E-6F);
    }
}
