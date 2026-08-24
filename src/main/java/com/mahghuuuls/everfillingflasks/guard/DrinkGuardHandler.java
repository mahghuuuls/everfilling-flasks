package com.mahghuuuls.everfillingflasks.guard;

import com.mahghuuuls.everfillingflasks.Tags;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerCapability;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The action lock: while drinking, a player cannot attack, break, place, interact, or use
 * items. One class answers one question, "is this player drinking", and cancels; the drinking
 * rules themselves stay in the controller. Most of these events fire on both sides and both
 * are cancelled, so the client does not even begin the animation it would have to roll back.
 *
 * <p>The hit interrupt lives here too: this class classifies the damage (attacker or not) and
 * forwards it; the threshold decision stays with the controller's frozen values.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class DrinkGuardHandler {

    private DrinkGuardHandler() {
    }

    private static boolean drinking(EntityPlayer player) {
        if (player.world.isRemote) {
            // Client capability data never holds drink state; the synced mirror does. The
            // client cancel is cosmetic (no predicted swing or door flicker); the server
            // cancel below stays the authority.
            return com.mahghuuuls.everfillingflasks.EverfillingFlasksMod.proxy
                    .isLocalPlayerDrinking(player);
        }
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        return data != null && data.drinking();
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (drinking(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (drinking(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (drinking(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (drinking(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (drinking(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (drinking(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() != null && drinking(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        if (event.getPlayer() != null && drinking(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntityLiving() instanceof EntityPlayer
                && drinking((EntityPlayer) event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    /**
     * The hit interrupt's classification point: only damage with an attacker is forwarded, and
     * this event's amount is post-armor because it fires after every reduction. The threshold
     * decision belongs to the controller, which holds the values frozen at drink start.
     */
    @SubscribeEvent
    public static void onLivingDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof net.minecraft.entity.player.EntityPlayerMP)) {
            return;
        }
        net.minecraft.entity.player.EntityPlayerMP player =
                (net.minecraft.entity.player.EntityPlayerMP) event.getEntityLiving();
        if (!drinking(player)) {
            // Combat is a hot path; non-drinkers exit before any further work.
            return;
        }
        net.minecraft.entity.Entity attacker = event.getSource().getTrueSource();
        if (attacker == null) {
            return;
        }
        com.mahghuuuls.everfillingflasks.player.DrinkController.hitTaken(
                player, event.getAmount(), attacker.getName());
    }
}
