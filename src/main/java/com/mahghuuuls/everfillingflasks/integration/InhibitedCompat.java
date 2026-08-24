package com.mahghuuuls.everfillingflasks.integration;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

/**
 * The whole Inhibited integration: while a player has the potion registered as
 * {@code inhibited:inhibited}, Flask recharge freezes. Detection is by registry name only, so
 * this compiles and runs with or without that mod installed, and no class of it is ever named.
 */
public final class InhibitedCompat {

    private static final ResourceLocation INHIBITED_POTION =
            new ResourceLocation("inhibited", "inhibited");

    private static boolean resolved;
    private static Potion potion;

    private InhibitedCompat() {
    }

    public static boolean isInhibited(EntityPlayer player) {
        if (!resolved) {
            // Lazy on purpose: first call is always after registry events, so load order
            // against the Inhibited mod cannot matter.
            potion = Potion.REGISTRY.getObject(INHIBITED_POTION);
            resolved = true;
            if (potion == null) {
                EverfillingFlasksMod.LOGGER.info(
                        "Inhibited is not installed; flask recharge is never frozen by it");
            }
        }
        return potion != null && player.isPotionActive(potion);
    }
}
