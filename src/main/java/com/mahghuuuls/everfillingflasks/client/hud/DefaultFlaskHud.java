package com.mahghuuuls.everfillingflasks.client.hud;

import com.mahghuuuls.everfillingflasks.Tags;
import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import com.mahghuuuls.everfillingflasks.api.client.FlaskHudRenderer;
import com.mahghuuuls.everfillingflasks.client.ClientFlaskState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The built-in Flask HUD: one small icon per charge slot, sitting above the health bar on the
 * left, with the next missing charge filling bottom-up as recharge progresses. Draws nothing
 * without an equipped Flask, no partial fill at maximum charges, and steps aside entirely when
 * a replacement renderer is registered.
 */
@SideOnly(Side.CLIENT)
public final class DefaultFlaskHud {

    private static final ResourceLocation FULL =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/hud_flask_full.png");
    private static final ResourceLocation EMPTY =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/hud_flask_empty.png");

    /** Icon size in points; matches the vanilla heart row's 9-point rhythm. */
    private static final int ICON = 9;
    /** Beyond this many charge slots the row becomes text, or it would cross the screen. */
    private static final int MAX_ICONS = 10;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HEALTH) {
            return;
        }
        FlaskSnapshot state = ClientFlaskState.snapshot();
        if (!state.hasFlask()) {
            return;
        }
        FlaskHudRenderer replacement = HudRegistry.replacement();
        if (replacement != null) {
            try {
                replacement.render(state, event.getResolution(), event.getPartialTicks());
            } catch (Throwable failure) {
                HudRegistry.reportReplacementFailure(failure);
            }
            reserveRow();
            restoreVanillaOverlayState();
            return;
        }
        if (HudRegistry.replacementRegistered()) {
            // The replacement failed earlier this session: the default stays suppressed, but
            // the row stays reserved so the other bars do not jump when a HUD breaks.
            reserveRow();
            return;
        }
        drawDefault(state, event.getResolution());
        reserveRow();
        restoreVanillaOverlayState();
    }

    /**
     * The overlay elements after HEALTH (food, armor, air) assume the vanilla icon sheet is
     * still bound and draw without rebinding it. Leaving our texture bound makes the hunger
     * bar sample a 9x9 flask picture and vanish, and the color left by text or rect drawing
     * would tint the next bar, so every path that drew anything restores both.
     */
    private static void restoreVanillaOverlayState() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawDefault(FlaskSnapshot state, ScaledResolution resolution) {
        Minecraft mc = Minecraft.getMinecraft();
        int left = resolution.getScaledWidth() / 2 - 91;
        int top = resolution.getScaledHeight() - GuiIngameForge.left_height;

        if (state.maxCharges() > MAX_ICONS) {
            // Drink progress is the cast bar's job in every mode; this row only shows charges.
            mc.fontRenderer.drawStringWithShadow(
                    state.charges() + " / " + state.maxCharges(), left, top, 0xFFFFFF);
            return;
        }
        for (int i = 0; i < state.maxCharges(); i++) {
            int x = left + i * (ICON + 1);
            mc.getTextureManager().bindTexture(EMPTY);
            Gui.drawModalRectWithCustomSizedTexture(x, top, 0, 0, ICON, ICON, ICON, ICON);
            if (i < state.charges()) {
                mc.getTextureManager().bindTexture(FULL);
                Gui.drawModalRectWithCustomSizedTexture(x, top, 0, 0, ICON, ICON, ICON, ICON);
            } else if (i == state.charges() && state.charges() < state.maxCharges()) {
                // The next missing charge fills bottom-up with recharge progress. At maximum
                // charges this branch is unreachable, so no misleading fill can appear.
                int filled = Math.round(ICON * progressFraction(state));
                if (filled > 0) {
                    mc.getTextureManager().bindTexture(FULL);
                    Gui.drawModalRectWithCustomSizedTexture(x, top + ICON - filled, 0,
                            ICON - filled, ICON, filled, ICON, ICON);
                }
            }
        }
    }

    private static float progressFraction(FlaskSnapshot state) {
        if (state.rechargeTicks() <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) state.rechargeProgressTicks() / state.rechargeTicks());
    }

    /** Moves the other left-side bars up, replacement or not, so layouts stay stable. */
    private static void reserveRow() {
        GuiIngameForge.left_height += ICON + 1;
    }
}
