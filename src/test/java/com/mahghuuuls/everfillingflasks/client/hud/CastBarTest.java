package com.mahghuuuls.everfillingflasks.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The cast bar's pure timing: the clamped fill and the linear interrupted fade. */
class CastBarTest {

    @Test
    void theFillIsSmoothAndClamped() {
        assertEquals(0.0F, CastBar.fill(0, 0.0F, 40), 1.0E-6F);
        assertEquals(0.5F, CastBar.fill(20, 0.0F, 40), 1.0E-6F);
        // Partial ticks slide the fill between whole ticks, which is what keeps a wide bar
        // from stepping visibly at high frame rates.
        assertTrue(CastBar.fill(20, 0.5F, 40) > 0.5F);
        assertEquals(1.0F, CastBar.fill(45, 0.9F, 40), 1.0E-6F);
        assertEquals(0.0F, CastBar.fill(10, 0.0F, 0), 1.0E-6F);
    }

    @Test
    void theInterruptedLookFadesLinearlyAndDies() {
        assertEquals(1.0F, CastBar.fadeAlpha(0), 1.0E-6F);
        assertEquals(0.5F, CastBar.fadeAlpha(CastBar.FADE_TICKS / 2), 1.0E-6F);
        assertEquals(0.0F, CastBar.fadeAlpha(CastBar.FADE_TICKS), 1.0E-6F);
        assertEquals(0.0F, CastBar.fadeAlpha(1000), 1.0E-6F);
        assertEquals(0.0F, CastBar.fadeAlpha(-1), 1.0E-6F);
    }
}
