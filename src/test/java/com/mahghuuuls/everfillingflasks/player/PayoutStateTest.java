package com.mahghuuuls.everfillingflasks.player;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The payout state's two rules (REQ-047): clearing forgets it entirely, and it never travels
 * through NBT, so a relog or a death clone starts with none.
 */
class PayoutStateTest {

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    void clearingForgetsThePayout() {
        FlaskPlayerData data = new FlaskPlayerData();
        data.payoutRemaining = 4.0F;
        data.payoutTicks = 100;
        assertTrue(data.payoutRunning());

        data.clearPayout();

        assertFalse(data.payoutRunning());
        assertEquals(0.0F, data.payoutRemaining, 0.0F);
        assertEquals(0, data.payoutTicks);
    }

    @Test
    void thePayoutIsNotPersisted() {
        FlaskPlayerData data = new FlaskPlayerData();
        data.payoutRemaining = 4.0F;
        data.payoutTicks = 100;

        NBTTagCompound tag = data.serializeNBT();
        FlaskPlayerData restored = new FlaskPlayerData();
        restored.deserializeNBT(tag);

        // A payout that came back after a relog would be the persistence rule broken.
        assertFalse(restored.payoutRunning());
        assertFalse(tag.hasKey("payoutRemaining"));
        assertFalse(tag.hasKey("payoutTicks"));
    }
}
