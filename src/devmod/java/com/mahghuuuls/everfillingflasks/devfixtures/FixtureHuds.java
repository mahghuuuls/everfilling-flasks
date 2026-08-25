package com.mahghuuuls.everfillingflasks.devfixtures;

import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import com.mahghuuuls.everfillingflasks.api.client.FlaskHudApi;
import com.mahghuuuls.everfillingflasks.api.client.FlaskHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The HUD-replacement fixtures, in their own class so the server never loads a client type:
 * the mod class calls {@link #register} only behind a side check.
 *
 * <p>The text HUD proves replacement (the default icons must vanish); the throwing HUD proves
 * the isolation: one error line, the default stays suppressed, the game continues.
 */
@SideOnly(Side.CLIENT)
final class FixtureHuds {

    private FixtureHuds() {
    }

    static void register(boolean throwing) {
        FlaskHudApi.setRenderer(throwing ? new ThrowingHud() : new TextHud());
    }

    /** The plain-text replacement: "Flask 2/4 70%". */
    static final class TextHud implements FlaskHudRenderer {

        @Override
        public void render(FlaskSnapshot state, ScaledResolution resolution, float partialTicks) {
            int percent = state.rechargeTicks() <= 0 ? 0
                    : 100 * state.rechargeProgressTicks() / state.rechargeTicks();
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(
                    "Flask " + state.charges() + "/" + state.maxCharges() + " " + percent + "%",
                    10, 10, 0x55FFFF);
        }
    }

    /** The hostile replacement: proves one broken HUD costs only itself. */
    static final class ThrowingHud implements FlaskHudRenderer {

        @Override
        public void render(FlaskSnapshot state, ScaledResolution resolution, float partialTicks) {
            throw new IllegalStateException("dev fixture: this HUD replacement always fails");
        }
    }
}
