package com.mahghuuuls.everfillingflasks.client.render;

import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The numbers behind the first-person drinking pose, reproduced from vanilla's private eat
 * transform so the Flask comes up to the face with the familiar bob. Vanilla drives its version
 * from the item-use countdown; ours runs from drink progress, so the inputs are converted here
 * and the trigonometry is kept pure and testable.
 */
@SideOnly(Side.CLIENT)
public final class DrinkTransforms {

    private DrinkTransforms() {
    }

    /**
     * How far the raise-to-face motion has come: 0 at the start of the drink, 1 at the lips.
     * Vanilla's curve: {@code 1 - remainingFraction^27}, which snaps up late in the drink.
     */
    public static float raise(float progressFraction) {
        float remaining = 1.0F - clamp(progressFraction);
        return 1.0F - (float) Math.pow(remaining, 27.0D);
    }

    /**
     * The vertical gulp bob. Vanilla enables it while the remaining fraction is under 0.8,
     * meaning after the first fifth of the drink, once the bottle is on the way up.
     * {@code remainingTicks} may carry partial ticks for smoothness.
     */
    public static float bob(float progressFraction, float remainingTicks) {
        if (1.0F - clamp(progressFraction) >= 0.8F) {
            return 0.0F;
        }
        return MathHelper.abs(MathHelper.cos(remainingTicks / 4.0F * (float) Math.PI) * 0.1F);
    }

    private static float clamp(float fraction) {
        return fraction < 0.0F ? 0.0F : fraction > 1.0F ? 1.0F : fraction;
    }
}
