package com.mahghuuuls.everfillingflasks.diagnostics;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * The diagnostic switch (REQ: one log line per Flask decision, off by default) and the fixed
 * wording of every line. All lines go through here so the vocabulary stays consistent and no
 * caller can accidentally log per tick: every method describes a discrete transition.
 */
public final class Diagnostics {

    private Diagnostics() {
    }

    public static boolean enabled() {
        return ConfigSnapshot.current().diagnostics();
    }

    public static void startingFlaskGranted(EntityPlayer player, ItemStack flask) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info("{}: starting flask granted: {}",
                player.getName(), describe(flask));
    }

    public static void chargeRestored(EntityPlayer player, int charges, int maxCharges) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info("{}: charge restored {}/{}",
                player.getName(), charges, maxCharges);
    }

    public static void rechargePaused(EntityPlayer player, int progressTicks, int rechargeTicks) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info("{}: recharge paused (inhibited) at {}/{} ticks",
                player.getName(), progressTicks, rechargeTicks);
    }

    public static void rechargeResumed(EntityPlayer player, int progressTicks, int rechargeTicks) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info("{}: recharge resumed at {}/{} ticks",
                player.getName(), progressTicks, rechargeTicks);
    }

    public static void drinkRefused(EntityPlayer player, String reason) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info("{}: drink refused: {}", player.getName(), reason);
    }

    public static void drinkStarted(EntityPlayer player, int drinkTicks, float healPercentage,
                                    float hitThreshold) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info(
                "{}: drink started (duration {} ticks, heal {}%, hit threshold {})",
                player.getName(), drinkTicks, Math.round(healPercentage * 100.0F), hitThreshold);
    }

    public static void drinkCancelled(EntityPlayer player, String reason) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info("{}: drink cancelled: {}", player.getName(), reason);
    }

    public static void drinkCompleted(EntityPlayer player, int charges, int maxCharges,
                                      float healed) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info(
                "{}: drink completed ({}/{} charges left, healed {} half-hearts)",
                player.getName(), charges, maxCharges, healed);
    }

    /**
     * One line per state message while recharging, which the sync cadence bounds to one per
     * second; the campaign counts these lines as the no-per-tick-networking evidence.
     */
    public static void stateSent(EntityPlayer player, int charges, int maxCharges,
                                 int progressTicks) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info("{}: state sent ({}/{} charges, progress {})",
                player.getName(), charges, maxCharges, progressTicks);
    }

    public static void slotChanged(EntityPlayer player, ItemStack from, ItemStack to) {
        if (!enabled()) {
            return;
        }
        EverfillingFlasksMod.LOGGER.info("{}: flask slot {} -> {}",
                player.getName(), describe(from), describe(to));
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        return stack.getItem().getRegistryName() + " (" + FlaskStackState.charges(stack) + " charges, "
                + FlaskStackState.progress(stack) + " progress)";
    }
}
