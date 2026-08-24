package com.mahghuuuls.everfillingflasks.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vanilla-derived drink pose curve at its endpoints: down at the start, at the lips at the
 * end, bob only after the first fifth. Would fail if the remaining-fraction conversion or the
 * exponent were dropped.
 */
class DrinkTransformsTest {

    @Test
    void theBottleStartsDownAndEndsAtTheLips() {
        assertEquals(0.0F, DrinkTransforms.raise(0.0F), 1.0E-6F);
        assertEquals(1.0F, DrinkTransforms.raise(1.0F), 1.0E-6F);
        assertTrue(DrinkTransforms.raise(0.5F) > 0.9F,
                "the curve snaps up well before the midpoint of the drink");
    }

    @Test
    void theBobWaitsForTheFirstFifth() {
        assertEquals(0.0F, DrinkTransforms.bob(0.1F, 27.0F), 1.0E-6F);
        // A phase where the cosine is near a peak: dropping the gate's else-branch, or zeroing
        // the amplitude, fails this.
        assertTrue(DrinkTransforms.bob(0.5F, 16.0F) > 0.05F,
                "inside the window the bob actually moves");
    }
}
