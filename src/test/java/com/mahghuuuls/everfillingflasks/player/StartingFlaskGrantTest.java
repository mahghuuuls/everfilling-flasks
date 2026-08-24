package com.mahghuuuls.everfillingflasks.player;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The validation order and outcomes of the starting-Flask decision, against the real item
 * registry. The grant application itself needs a live player and is observed in the runtime
 * campaign; everything that can refuse is covered here.
 */
class StartingFlaskGrantTest {

    @BeforeAll
    static void bootstrapAndRegisterAFlask() {
        Bootstrap.register();
        // Refusal of a duplicate is fine: another test class may have used this item already.
        FlaskRegistry.register(Items.DRAGON_BREATH, new FlaskDefinition() {
            @Override
            public int maxCharges(ItemStack stack, EntityPlayer player) {
                return 2;
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
        });
    }

    @Test
    void anEmptyValueIsDisabledNotAnError() {
        assertEquals(StartingFlaskGrant.Outcome.DISABLED, StartingFlaskGrant.decide("").outcome);
    }

    @Test
    void aMalformedNameResolvesToNoItem() {
        // This Minecraft version's ResourceLocation accepts any string without validating it,
        // so a malformed value is indistinguishable from a missing item and warns as one.
        assertEquals(StartingFlaskGrant.Outcome.NO_SUCH_ITEM,
                StartingFlaskGrant.decide("everfillingflasks:Common Flask!").outcome);
    }

    @Test
    void anUnknownItemIsRefusedAsMissing() {
        assertEquals(StartingFlaskGrant.Outcome.NO_SUCH_ITEM,
                StartingFlaskGrant.decide("nosuchmod:flask").outcome);
    }

    @Test
    void aRealItemThatIsNotAFlaskIsRefused() {
        // Bedrock, because other test classes register sticks and bottles as Flasks in the
        // shared static registry, and this test must not depend on their choices.
        StartingFlaskGrant.Decision decision = StartingFlaskGrant.decide("minecraft:bedrock");
        assertEquals(StartingFlaskGrant.Outcome.NOT_A_FLASK, decision.outcome);
        assertNull(decision.item);
    }

    @Test
    void aRegisteredFlaskGrants() {
        StartingFlaskGrant.Decision decision = StartingFlaskGrant.decide("minecraft:dragon_breath");
        assertEquals(StartingFlaskGrant.Outcome.GRANT, decision.outcome);
        assertSame(Items.DRAGON_BREATH, decision.item);
    }
}
