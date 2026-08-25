package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskApi;
import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The facade's binding contract, in one method because the bridge binds once per JVM: before
 * the bind, queries answer safely and registrations buffer; at the bind, buffered
 * registrations land; after it, calls flow straight through.
 */
class FlaskApiFacadeTest {

    private static final class ChorusFlask implements FlaskDefinition {
        @Override
        public int maxCharges(ItemStack stack, EntityPlayer player) {
            return 2;
        }

        @Override
        public float healPercentage(ItemStack stack, EntityPlayer player) {
            return 0.1F;
        }

        @Override
        public int rechargeTicks(ItemStack stack, EntityPlayer player) {
            return 100;
        }

        @Override
        public int drinkTicks(ItemStack stack, EntityPlayer player) {
            return 10;
        }

        @Override
        public float hitThreshold(ItemStack stack, EntityPlayer player) {
            return 1.0F;
        }
    }

    @Test
    void queriesAnswerSafelyBeforeTheBindAndBufferedRegistrationsLandAtIt() {
        Bootstrap.register();
        ItemStack chorus = new ItemStack(Items.CHORUS_FRUIT);

        // Before the bind: no throw, no recognition, an empty snapshot.
        assertFalse(FlaskApi.isFlask(chorus));
        assertNull(FlaskApi.definition(chorus));
        FlaskSnapshot preBind = FlaskApi.snapshot(null);
        assertFalse(preBind.hasFlask());

        ChorusFlask definition = new ChorusFlask();
        FlaskApi.registerFlask(Items.CHORUS_FRUIT, definition);
        assertFalse(FlaskApi.isFlask(chorus),
                "still unbound: the registration is buffered, not applied");

        InternalFlaskApiBridge.install();

        assertTrue(FlaskApi.isFlask(chorus), "the buffered registration landed at the bind");
        assertSame(definition, FlaskApi.definition(chorus));
        assertTrue(FlaskRegistry.isFlask(chorus),
                "and it landed in the real internal registry");
    }
}
