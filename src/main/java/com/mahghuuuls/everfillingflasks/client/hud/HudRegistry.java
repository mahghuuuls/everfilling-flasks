package com.mahghuuuls.everfillingflasks.client.hud;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.api.client.FlaskHudRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The one HUD renderer slot. No replacement registered means the default Flask HUD draws;
 * a registered replacement suppresses the default entirely. There is deliberately no layering
 * and no unregister: the last registration wins and is logged.
 *
 * <p>A replacement that throws is logged once and skipped from then on, with the default still
 * suppressed: half a custom HUD over the default would be worse than neither.
 */
@SideOnly(Side.CLIENT)
public final class HudRegistry {

    private static FlaskHudRenderer replacement;
    private static boolean replacementFailed;

    private HudRegistry() {
    }

    public static void setRenderer(FlaskHudRenderer renderer) {
        if (replacement != null && renderer != null) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Flask HUD replacement {} replaces previously registered {}",
                    renderer.getClass().getName(), replacement.getClass().getName());
        }
        replacement = renderer;
        replacementFailed = false;
    }

    /** The renderer to use this frame: the healthy replacement, or null for the default. */
    @Nullable
    static FlaskHudRenderer replacement() {
        return replacementFailed ? null : replacement;
    }

    static boolean replacementRegistered() {
        return replacement != null;
    }

    static void reportReplacementFailure(Throwable failure) {
        if (!replacementFailed) {
            replacementFailed = true;
            EverfillingFlasksMod.LOGGER.error(
                    "Flask HUD replacement {} failed and is disabled for this session; the"
                            + " default HUD stays suppressed",
                    replacement == null ? "<none>" : replacement.getClass().getName(), failure);
        }
    }
}
