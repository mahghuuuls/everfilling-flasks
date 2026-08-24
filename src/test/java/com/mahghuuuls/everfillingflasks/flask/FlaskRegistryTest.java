package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The recognition contract: registration is the only path to Flask-ness, duplicates are refused
 * with the first registration kept, and refusals return instead of throwing.
 */
class FlaskRegistryTest {

    private static final FlaskDefinition DEFINITION = new FixedDefinition();
    private static final FlaskDefinition SECOND = new FixedDefinition();

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    void registrationMakesAnItemAFlask() {
        assertTrue(FlaskRegistry.register(Items.BLAZE_ROD, DEFINITION));
        assertTrue(FlaskRegistry.isFlask(new ItemStack(Items.BLAZE_ROD)));
        assertSame(DEFINITION, FlaskRegistry.definition(new ItemStack(Items.BLAZE_ROD)));
    }

    @Test
    void anUnregisteredItemIsNotAFlask() {
        assertFalse(FlaskRegistry.isFlask(new ItemStack(Items.POTIONITEM)));
        assertNull(FlaskRegistry.definition(new ItemStack(Items.POTIONITEM)));
        assertFalse(FlaskRegistry.isFlask(ItemStack.EMPTY));
    }

    @Test
    void aDuplicateRegistrationIsRefusedAndTheFirstKept() {
        assertTrue(FlaskRegistry.register(Items.STICK, DEFINITION));
        assertFalse(FlaskRegistry.register(Items.STICK, SECOND));
        assertSame(DEFINITION, FlaskRegistry.definition(new ItemStack(Items.STICK)));
    }

    @Test
    void nullRegistrationsAreRefusedWithoutThrowing() {
        assertFalse(FlaskRegistry.register(null, DEFINITION));
        assertFalse(FlaskRegistry.register(Items.APPLE, null));
        assertFalse(FlaskRegistry.isFlask(new ItemStack(Items.APPLE)));
    }

    private static final class FixedDefinition implements FlaskDefinition {

        @Override
        public int maxCharges(ItemStack stack, EntityPlayer player) {
            return 4;
        }

        @Override
        public float healPercentage(ItemStack stack, EntityPlayer player) {
            return 0.3F;
        }

        @Override
        public int rechargeTicks(ItemStack stack, EntityPlayer player) {
            return 1200;
        }

        @Override
        public int drinkTicks(ItemStack stack, EntityPlayer player) {
            return 30;
        }

        @Override
        public float hitThreshold(ItemStack stack, EntityPlayer player) {
            return 1.0F;
        }
    }
}
