package com.mahghuuuls.everfillingflasks.client.hud;

import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import com.mahghuuuls.everfillingflasks.client.ClientFlaskState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The drink cast bar: boss-bar wide, centered, a little below the crosshair, filling left to
 * right while the key is held. An interruption freezes the bar at its last fill, turns it red,
 * and fades it out over {@link CastBar#FADE_TICKS} ticks; a completion simply removes it, the
 * completion burst being the celebratory half. Idle draws nothing, which is what makes a
 * refused drink visibly different from a slow one.
 *
 * <p>Steps aside whenever a HUD replacement is registered: the snapshot already carries the
 * drink progress, so a replacement owns the whole presentation including its own bar.
 */
@SideOnly(Side.CLIENT)
public final class CastBarRenderer {

    /** Vanilla boss bar width in points. */
    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;

    private static final int FILL = 0xE8A33C;
    private static final int FILL_INTERRUPTED = 0xD03030;
    private static final int BACKGROUND = 0x101010;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        if (HudRegistry.replacementRegistered()) {
            return;
        }
        ScaledResolution resolution = event.getResolution();
        int left = resolution.getScaledWidth() / 2 - WIDTH / 2;
        int top = Math.round(resolution.getScaledHeight() * 0.6F);

        FlaskSnapshot state = ClientFlaskState.snapshot();
        if (state.drinking() && state.drinkTicks() > 0) {
            float fill = CastBar.fill(state.drinkProgressTicks(), event.getPartialTicks(),
                    state.drinkTicks());
            drawBar(left, top, fill, FILL, 1.0F);
        } else {
            float alpha = CastBar.fadeAlpha(ClientFlaskState.ticksSinceInterrupt());
            if (alpha > 0.0F) {
                drawBar(left, top, ClientFlaskState.interruptedFill(), FILL_INTERRUPTED, alpha);
            }
        }
    }

    private static void drawBar(int left, int top, float fill, int fillColor, float alpha) {
        int backgroundAlpha = (int) (alpha * 0xA0) << 24;
        int fillAlpha = (int) (alpha * 0xFF) << 24;
        Gui.drawRect(left, top, left + WIDTH, top + HEIGHT, backgroundAlpha | BACKGROUND);
        int filledWidth = Math.round(WIDTH * Math.min(1.0F, fill));
        if (filledWidth > 0) {
            Gui.drawRect(left, top, left + filledWidth, top + HEIGHT, fillAlpha | fillColor);
        }
        // drawRect leaves its last color in the GL state; a following overlay would be tinted.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
