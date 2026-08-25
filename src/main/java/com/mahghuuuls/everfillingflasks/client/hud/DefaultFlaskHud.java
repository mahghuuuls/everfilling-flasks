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

    private static final ResourceLocation GLASS =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/hud_flask_empty.png");
    private static final ResourceLocation LIQUID =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/hud_flask_liquid.png");

    /** The built-in liquid red, multiplying the white liquid mask. */
    private static final int DEFAULT_LIQUID = 0xDE382D;

    /** Icon size in points; matches the vanilla heart row's 9-point rhythm. */
    private static final int ICON = 9;
    /**
     * The liquid mask's opaque rows (the bottle interior) and the clear rows under them, in
     * the icon's nine-point space. The textures are 18x18 files drawn at nine points, so the
     * interior spans point rows 3 to 7; UVs are normalized, so the file resolution is free.
     */
    private static final int LIQUID_ROWS = 5;
    private static final int ROWS_BELOW_MASK = 1;
    /** Beyond this many charge slots the row becomes text, or it would cross the screen. */
    private static final int MAX_ICONS = 10;

    private static boolean definitionHudFailed;

    /**
     * The three per-Flask HUD choices, read together with one isolation: a definition that
     * throws here is logged once and the defaults draw from then on, because the HUD runs
     * every frame and must never crash rendering.
     */
    private static final class IconStyle {
        ResourceLocation glass = GLASS;
        ResourceLocation liquid = LIQUID;
        float red; float green; float blue;

        IconStyle(FlaskSnapshot state) {
            int color = DEFAULT_LIQUID;
            if (!definitionHudFailed) {
                try {
                    com.mahghuuuls.everfillingflasks.api.FlaskDefinition definition =
                            com.mahghuuuls.everfillingflasks.flask.FlaskRegistry
                                    .definition(state.flask());
                    if (definition != null) {
                        net.minecraft.entity.player.EntityPlayer player =
                                Minecraft.getMinecraft().player;
                        int custom = definition.hudLiquidColor(state.flask(), player);
                        ResourceLocation customGlass =
                                definition.hudGlassTexture(state.flask(), player);
                        ResourceLocation customLiquid =
                                definition.hudLiquidTexture(state.flask(), player);
                        if (customGlass != null) {
                            glass = customGlass;
                        }
                        if (customLiquid != null) {
                            liquid = customLiquid;
                            // On a custom layer, -1 means untinted, not the built-in red.
                            color = custom >= 0 ? custom & 0xFFFFFF : 0xFFFFFF;
                        } else if (custom >= 0) {
                            color = custom & 0xFFFFFF;
                        }
                    }
                } catch (Throwable failure) {
                    definitionHudFailed = true;
                    com.mahghuuuls.everfillingflasks.EverfillingFlasksMod.LOGGER.error(
                            "A Flask definition failed in its HUD methods; the default icon"
                                    + " style is used for the rest of the session", failure);
                }
            }
            red = (color >> 16 & 0xFF) / 255.0F;
            green = (color >> 8 & 0xFF) / 255.0F;
            blue = (color & 0xFF) / 255.0F;
        }
    }

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
        IconStyle style = new IconStyle(state);
        for (int i = 0; i < state.maxCharges(); i++) {
            int x = left + i * (ICON + 1);
            mc.getTextureManager().bindTexture(style.glass);
            Gui.drawModalRectWithCustomSizedTexture(x, top, 0, 0, ICON, ICON, ICON, ICON);
            if (i < state.charges()) {
                drawLiquid(mc, style, x, top, ICON);
            } else if (i == state.charges() && state.charges() < state.maxCharges()) {
                // The next missing charge fills bottom-up with recharge progress, mapped onto
                // the liquid mask's own rows so every step is visible, and capped one row
                // short of full so a recharging icon never mimics an earned charge. At
                // maximum charges this branch is unreachable.
                int level = Math.min(LIQUID_ROWS - 1,
                        Math.round(LIQUID_ROWS * progressFraction(state)));
                if (level > 0) {
                    int height = level + ROWS_BELOW_MASK;
                    drawLiquid(mc, style, x, top + ICON - height, height);
                }
            }
        }
    }

    /** The tinted liquid layer, full or cropped bottom-up; the tint never leaks out. */
    private static void drawLiquid(Minecraft mc, IconStyle style, int x, int y, int height) {
        mc.getTextureManager().bindTexture(style.liquid);
        GlStateManager.color(style.red, style.green, style.blue, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, ICON - height, ICON, height,
                ICON, ICON);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
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
