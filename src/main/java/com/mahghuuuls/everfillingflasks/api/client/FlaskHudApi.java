package com.mahghuuuls.everfillingflasks.api.client;

import com.mahghuuuls.everfillingflasks.client.hud.HudRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client half of the public API: replacing the Flask HUD. Client-only, like everything in
 * this package — never load this class in code a dedicated server can reach; register from
 * your client proxy or a client-sided event handler.
 *
 * <p>Registering a renderer suppresses the default HUD entirely, including its cast bar;
 * there is no layering, and the last registration wins with a log line. A renderer that
 * throws is disabled for the rest of the session while the default stays suppressed, so a
 * broken replacement fails visibly rather than half-drawing.
 */
@SideOnly(Side.CLIENT)
public final class FlaskHudApi {

    private FlaskHudApi() {
    }

    /**
     * Installs {@code renderer} as the Flask HUD. Null restores the default. May be called at
     * any time, including before this mod's client setup.
     */
    public static void setRenderer(FlaskHudRenderer renderer) {
        HudRegistry.setRenderer(renderer);
    }
}
