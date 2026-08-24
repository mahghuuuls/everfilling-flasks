package com.mahghuuuls.everfillingflasks.api.client;

import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A complete replacement for the default Flask HUD. Registering one suppresses the default
 * entirely; there is no layering. Called once per frame during the game overlay, on the client
 * render thread, with a read-only state snapshot.
 *
 * <p>Client-only, like everything in this package: never load this class in code a dedicated
 * server can reach.
 *
 * <p>An implementation that throws is disabled for the rest of the session and the default HUD
 * stays suppressed, so a broken replacement fails visibly rather than half-drawing.
 */
@SideOnly(Side.CLIENT)
public interface FlaskHudRenderer {

    void render(FlaskSnapshot state, ScaledResolution resolution, float partialTicks);
}
