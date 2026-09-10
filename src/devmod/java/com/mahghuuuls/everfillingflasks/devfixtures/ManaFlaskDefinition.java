package com.mahghuuuls.everfillingflasks.devfixtures;

import com.mahghuuuls.everfillingflasks.api.DrinkOutcome;
import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;

/**
 * The Mana Flask's behavior, using only the public API surface:
 *
 * <ul>
 * <li>Maximum charges depend on the stack's own NBT ({@code fixture.tier}: 2 plus the tier),
 * proving per-stack values reach every computation.</li>
 * <li>Heal 0 by default: the drink is worth taking only for its completion hook, and works at
 * full health. {@code fixture.heal} (whole percent) gives it an instant heal for the outcome
 * cards.</li>
 * <li>{@code fixture.overTimePct} and {@code fixture.overTimeTicks} declare an over-time heal
 * (1.1.0, REQ-047), so one command hands over a Flask that pays out.</li>
 * <li>{@code fixture.outcome} (1b) makes the hook take the outcome form (1.1.0, REQ-049) and
 * say every field in chat and in the log; without it the hook is the 1.0.0 two-argument form,
 * reached through the core's default forwarding.</li>
 * <li>The hook grants 5 seconds of Speed either way, standing in for "mana".</li>
 * <li>{@code -Deff.devfixtures.throwinghook=true}: the hook throws instead, for watching the
 * isolation leave charges and health correct with one log line.</li>
 * <li>{@code -Deff.devfixtures.quietflask=true}: completion feedback disabled through the API
 * switch, for watching a completion with no burst and no chime.</li>
 * </ul>
 */
final class ManaFlaskDefinition implements FlaskDefinition {

    private static NBTTagCompound fixture(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("fixture")) {
            return new NBTTagCompound();
        }
        return tag.getCompoundTag("fixture");
    }

    private static int tier(ItemStack stack) {
        return Math.max(0, fixture(stack).getInteger("tier"));
    }

    @Override
    public int maxCharges(ItemStack stack, EntityPlayer player) {
        return 2 + tier(stack);
    }

    /**
     * Infusion slots from the stack's own NBT ({@code fixture.slots}), so one command can hand
     * over a Flask with three slots and another with twelve. Zero, the default, means the
     * definition says nothing and the Flask gets the usual six.
     */
    @Override
    public int infusionSlots(ItemStack stack) {
        int declared = fixture(stack).getInteger("slots");
        return declared <= 0 ? 6 : declared;
    }

    @Override
    public float healPercentage(ItemStack stack, EntityPlayer player) {
        return fixture(stack).getInteger("heal") / 100.0F;
    }

    @Override
    public float healOverTimePercentage(ItemStack stack, EntityPlayer player) {
        return fixture(stack).getInteger("overTimePct") / 100.0F;
    }

    @Override
    public int healOverTimeTicks(ItemStack stack, EntityPlayer player) {
        return fixture(stack).getInteger("overTimeTicks");
    }

    @Override
    public int rechargeTicks(ItemStack stack, EntityPlayer player) {
        return 200;
    }

    @Override
    public int drinkTicks(ItemStack stack, EntityPlayer player) {
        return 20;
    }

    @Override
    public float hitThreshold(ItemStack stack, EntityPlayer player) {
        return 1.0F;
    }

    /** The 1.0.0 form: what an add-on compiled against 1.0.0 still overrides. */
    @Override
    public void onDrinkCompleted(ItemStack stack, EntityPlayer player) {
        if (Boolean.getBoolean("eff.devfixtures.throwinghook")) {
            throw new IllegalStateException("dev fixture: this completion hook always fails");
        }
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 0));
    }

    /**
     * The 1.1.0 form. Without {@code fixture.outcome} it hands over to the plain form the way
     * the core's own default would, so the same item serves both cards. With it, every outcome
     * field goes to chat (for the owner) and to the log (for the agent) before the plain
     * behaviour runs.
     */
    @Override
    public void onDrinkCompleted(ItemStack stack, EntityPlayer player, DrinkOutcome outcome) {
        if (fixture(stack).getBoolean("outcome")) {
            String line = String.format(
                    "outcome: healthBefore=%.1f requested=%.1f applied=%.1f overTime=%.1f"
                            + " chargesLeft=%d effectPower=%.2f",
                    outcome.healthBefore(), outcome.healRequested(), outcome.healApplied(),
                    outcome.healOverTimeScheduled(), outcome.chargesLeft(),
                    outcome.effectPower());
            DevFixturesMod.LOGGER.info("{}: {}", player.getName(), line);
            player.sendMessage(new TextComponentString(line));
        }
        onDrinkCompleted(stack, player);
    }

    @Override
    public boolean completionEffect(ItemStack stack, EntityPlayer player) {
        return !Boolean.getBoolean("eff.devfixtures.quietflask");
    }

    @Override
    public boolean completionSound(ItemStack stack, EntityPlayer player) {
        return !Boolean.getBoolean("eff.devfixtures.quietflask");
    }

    @Override
    public String journalText(ItemStack stack) {
        // Proves the "Where to Find" path, and that a pack can override it by registry name.
        return "everfillingflasksdev.journal.mana.hint";
    }

    @Override
    public int hudLiquidColor(ItemStack stack, EntityPlayer player) {
        // Mana is blue: proves the per-Flask liquid tint reaches the default HUD.
        return 0x3F7FFF;
    }
}
