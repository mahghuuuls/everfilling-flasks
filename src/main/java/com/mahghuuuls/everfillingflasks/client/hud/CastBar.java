package com.mahghuuuls.everfillingflasks.client.hud;

/**
 * The cast bar's pure timing math, free of any Minecraft class so the fill and the
 * interrupted fade are testable against plain numbers.
 */
public final class CastBar {

    /** Ticks the interrupted look takes to fade out completely. */
    public static final int FADE_TICKS = 10;

    private CastBar() {
    }

    /** Fill fraction 0..1 for an active drink, smooth via partial ticks, clamped at full. */
    public static float fill(int progressTicks, float partialTicks, int drinkTicks) {
        if (drinkTicks <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (progressTicks + partialTicks) / drinkTicks);
    }

    /**
     * Opacity of the interrupted look, 1 at the moment of interruption falling linearly to 0
     * at {@link #FADE_TICKS}; 0 forever after, which is also the idle answer.
     */
    public static float fadeAlpha(int ticksSinceInterrupt) {
        if (ticksSinceInterrupt < 0 || ticksSinceInterrupt >= FADE_TICKS) {
            return 0.0F;
        }
        return 1.0F - (float) ticksSinceInterrupt / FADE_TICKS;
    }
}
