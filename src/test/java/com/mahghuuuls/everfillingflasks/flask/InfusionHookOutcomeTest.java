package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.DrinkOutcome;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The 1.2.0 infusion hook contract (REQ-052): dispatch goes through the outcome form, a 1.1.0
 * definition still runs exactly once per placed piece, and a 1.2.0 definition receives the very
 * outcome object the dispatcher was given.
 */
class InfusionHookOutcomeTest {

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

    /** Written against 1.1.0: overrides only the three-argument hook. */
    private static final class PlainHook implements InfusionDefinition {
        int calls;

        @Override
        public int potencyCost(ItemStack infusion) {
            return 1;
        }

        @Override
        public void onDrinkCompleted(ItemStack infusion, ItemStack flask, EntityPlayer player) {
            calls++;
        }
    }

    /** Written against 1.2.0: overrides the outcome form and keeps what it was given. */
    private static final class OutcomeHook implements InfusionDefinition {
        DrinkOutcome seen;
        int calls;

        @Override
        public int potencyCost(ItemStack infusion) {
            return 1;
        }

        @Override
        public void onDrinkCompleted(ItemStack infusion, ItemStack flask, EntityPlayer player,
                                     DrinkOutcome outcome) {
            calls++;
            seen = outcome;
        }
    }

    private static NonNullList<ItemStack> grid(ItemStack... pieces) {
        NonNullList<ItemStack> grid = NonNullList.withSize(5, ItemStack.EMPTY);
        for (int i = 0; i < pieces.length; i++) {
            grid.set(i, pieces[i]);
        }
        return grid;
    }

    @Test
    void aPlainHookRunsOncePerPlacedPieceThroughTheDefault() {
        PlainHook plain = new PlainHook();
        InfusionRegistry.register(Items.CLAY_BALL, plain);
        DrinkOutcome outcome = new DrinkOutcome(10.0F, 6.0F, 6.0F, 0.0F, 1, 1.0F);

        InfusionRegistry.dispatchDrinkCompleted(
                grid(new ItemStack(Items.CLAY_BALL), new ItemStack(Items.CLAY_BALL)),
                ItemStack.EMPTY, null, outcome);

        // Would be 0 without the default forwarding, 4 if the dispatcher called both forms.
        assertEquals(2, plain.calls);
    }

    @Test
    void anOutcomeHookReceivesTheDispatchedOutcomeItself() {
        OutcomeHook hook = new OutcomeHook();
        InfusionRegistry.register(Items.BRICK, hook);
        DrinkOutcome outcome = new DrinkOutcome(8.0F, 6.0F, 6.0F, 0.0F, 2, 1.5F);

        InfusionRegistry.dispatchDrinkCompleted(grid(new ItemStack(Items.BRICK)),
                ItemStack.EMPTY, null, outcome);

        assertEquals(1, hook.calls);
        // The same object, so its effect power is the flask's own value, not a copy.
        assertSame(outcome, hook.seen);
        assertEquals(1.5F, hook.seen.effectPower(), 1.0E-6F);
    }

    @Test
    void aThrowingHookCostsOnlyItsOwnPiece() {
        InfusionRegistry.register(Items.FLINT, new InfusionDefinition() {
            @Override
            public int potencyCost(ItemStack infusion) {
                return 1;
            }

            @Override
            public void onDrinkCompleted(ItemStack infusion, ItemStack flask,
                                         EntityPlayer player, DrinkOutcome outcome) {
                throw new IllegalStateException("test hook that always fails");
            }
        });
        PlainHook plain = new PlainHook();
        InfusionRegistry.register(Items.CLAY_BALL, plain);

        InfusionRegistry.dispatchDrinkCompleted(
                grid(new ItemStack(Items.FLINT), new ItemStack(Items.CLAY_BALL)),
                ItemStack.EMPTY, null, new DrinkOutcome(0, 0, 0, 0, 0, 1.0F));

        assertEquals(1, plain.calls);
    }
}
