package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.FlaskModifierSource;
import net.minecraft.entity.player.EntityPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The isolation contract: one throwing source costs its own bonuses and nothing else, and a
 * null registration is refused without an exception.
 */
class ModifierRegistryTest {

    @AfterEach
    void clearRegistry() {
        ModifierRegistry.clearForTests();
    }

    @Test
    void aThrowingSourceIsSkippedAndTheOthersStillApply() {
        ModifierRegistry.register(new FlaskModifierSource() {
            @Override
            public void contribute(EntityPlayer player, FlaskBonuses bonuses) {
                throw new IllegalStateException("test source that always fails");
            }
        });
        ModifierRegistry.register(new FlaskModifierSource() {
            @Override
            public void contribute(EntityPlayer player, FlaskBonuses bonuses) {
                bonuses.healing(0.5F);
            }
        });

        FlaskBonuses collected = ModifierRegistry.collect(null);

        // Would fail if the throwing source aborted collection or poisoned the accumulator.
        assertEquals(0.5F, collected.healingSum(), 1.0E-6F);
    }

    @Test
    void aNullRegistrationIsRefusedWithoutThrowing() {
        ModifierRegistry.register(null);
        ModifierRegistry.collect(null);
    }
}
