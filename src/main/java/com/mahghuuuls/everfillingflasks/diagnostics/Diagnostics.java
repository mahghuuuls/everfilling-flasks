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
