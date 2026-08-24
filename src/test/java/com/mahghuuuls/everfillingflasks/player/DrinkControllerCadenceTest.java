package com.mahghuuuls.everfillingflasks.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cadence predicate behind both once-per-second rules (NBT flush and steady-state sync).
 * The overflow case is here because it happened: a {@code Long.MIN_VALUE} sentinel made
 * {@code now - last} wrap negative, and the flush silently never fired until the first charge
 * rollover.
 */
class DrinkControllerCadenceTest {

    @Test
    void aFullPeriodMakesTheClockDue() {
        assertTrue(DrinkController.due(120, 100));
        assertFalse(DrinkController.due(119, 100));
    }

    @Test
    void anUninitializedOrWrappedClockIsDueInsteadOfSilentlyNeverFiring() {
        // Would fail if the predicate were written as the plain subtraction, which overflows.
        assertTrue(DrinkController.due(100, Long.MIN_VALUE));
        // A recorded time ahead of the world clock (world change, backup restore) is also due.
        assertTrue(DrinkController.due(100, 500));
    }

    @Test
    void aFreshPlayerFlushesWithinTheFirstPeriodOfARealWorldClock() {
        // Fresh capability clocks start at zero; on a long-running world the first flush must
        // be due immediately, not after a rollover.
        assertTrue(DrinkController.due(1000000, 0));
    }

    @Test
    void theFlushTargetsTheTrackedStackNeverTheSlotsCurrentContent() {
        // Between a container click and the next tick's reconciliation the slot can hold a
        // different Flask than the one whose live progress is in the capability. A flush into
        // the slot's current content would duplicate recharge progress across two Flasks; this
        // failed before the fix that targets the tracked stack.
        net.minecraft.init.Bootstrap.register();
        FlaskPlayerData data = new FlaskPlayerData();
        net.minecraft.item.ItemStack tracked =
                new net.minecraft.item.ItemStack(net.minecraft.init.Items.GLASS_BOTTLE);
        net.minecraft.item.ItemStack swappedIn =
                new net.minecraft.item.ItemStack(net.minecraft.init.Items.POTIONITEM);
        data.slot().setStackInSlot(0, swappedIn);
        data.trackedStack = tracked;
        data.liveProgress = 1180;
        data.liveValid = true;

        data.flushLiveProgress();

        org.junit.jupiter.api.Assertions.assertEquals(1180,
                com.mahghuuuls.everfillingflasks.flask.FlaskStackState.progress(tracked),
                "the departing tracked stack receives its live progress");
        org.junit.jupiter.api.Assertions.assertEquals(0,
                com.mahghuuuls.everfillingflasks.flask.FlaskStackState.progress(
                        data.equippedFlask()),
                "the freshly placed stack must not inherit another flask's progress");
    }

    @Test
    void theCadenceBoundsWritesToOnePerPeriodOverALongRun() {
        // The fake-clock write counter: 200 ticks may produce at most 10 writes, and exactly
        // 10 when progress advances every tick.
        long last = 0;
        int writes = 0;
        for (long now = 1000; now < 1200; now++) {
            if (DrinkController.due(now, last)) {
                writes++;
                last = now;
            }
        }
        assertEquals(10, writes);
    }
}
