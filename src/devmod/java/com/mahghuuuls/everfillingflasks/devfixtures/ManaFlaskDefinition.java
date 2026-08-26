package com.mahghuuuls.everfillingflasks.devfixtures;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;

/**
 * The Mana Flask's behavior, using only the public API surface:
 *
 * <ul>
 * <li>Maximum charges depend on the stack's own NBT ({@code fixture.tier}: 2 plus the tier),
 * proving per-stack values reach every computation.</li>
 * <li>Heal 0: the drink is worth taking only for its completion hook, and works at full
 * health.</li>
 * <li>The hook grants 5 seconds of Speed, standing in for "mana".</li>
 * <li>{@code -Deff.devfixtures.throwinghook=true}: the hook throws instead, for watching the
 * isolation leave charges and health correct with one log line.</li>
 * <li>{@code -Deff.devfixtures.quietflask=true}: completion feedback disabled through the API
 * switch, for watching a completion with no burst and no chime.</li>
 * </ul>
 */
final class ManaFlaskDefinition implements FlaskDefinition {

    private static int tier(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("fixture")) {
            return 0;
        }
        return Math.max(0, tag.getCompoundTag("fixture").getInteger("tier"));
    }

    @Override
    public int maxCharges(ItemStack stack, EntityPlayer player) {
        return 2 + tier(stack);
    }

    @Override
    public float healPercentage(ItemStack stack, EntityPlayer player) {
        return 0.0F;
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

    @Override
    public void onDrinkCompleted(ItemStack stack, EntityPlayer player) {
        if (Boolean.getBoolean("eff.devfixtures.throwinghook")) {
            throw new IllegalStateException("dev fixture: this completion hook always fails");
        }
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, 0));
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
