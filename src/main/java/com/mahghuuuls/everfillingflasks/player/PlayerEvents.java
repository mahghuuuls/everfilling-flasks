package com.mahghuuuls.everfillingflasks.player;

import com.mahghuuuls.everfillingflasks.Tags;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.diagnostics.Diagnostics;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Lifecycle wiring for the player capability: attach, carry across death and the End return,
 * the login-time starting-Flask grant, the per-tick recharge dispatch, and the state sync at
 * every lifecycle boundary the client cannot infer.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PlayerEvents {

    private static final ResourceLocation CAPABILITY_KEY =
            new ResourceLocation(Tags.MOD_ID, "flask_player_data");

    private PlayerEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(CAPABILITY_KEY, new FlaskPlayerProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        // The event fires only on the server, from PlayerList; no side check is needed.
        StartingFlaskGrant.tryGrant(event.player);
        if (event.player instanceof EntityPlayerMP) {
            // A crash mid-drink must not leave the slowdown on a saved attribute map.
            DrinkController.removeSlowdown((EntityPlayerMP) event.player);
            DrinkController.syncNow((EntityPlayerMP) event.player);
        }
    }

    /** The recharge engine runs once per player per server tick, after the world has moved. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof EntityPlayerMP) {
            DrinkController.tick((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            DrinkController.cancelDrink((EntityPlayerMP) event.player, "logged out");
            DrinkController.flush((EntityPlayerMP) event.player);
        }
    }

    /** Dying cancels the drink; the clone handler separately decides the Flask itself. Lowest
     * priority so a totem or another mod un-cancelling the death is seen first. */
    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntityLiving() instanceof EntityPlayerMP) {
            DrinkController.cancelDrink((EntityPlayerMP) event.getEntityLiving(), "died");
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            DrinkController.syncNow((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            DrinkController.cancelDrink((EntityPlayerMP) event.player, "changed dimension");
            DrinkController.syncNow((EntityPlayerMP) event.player);
        }
    }

    /**
     * Vanilla builds a fresh player entity on death and on return from the End, so the
     * capability must be copied over by hand. The granted flag always survives; whether the
     * Flask itself survives death is the owner's config. When it does not survive, it follows
     * the rest of the inventory: kept under keepInventory, dropped otherwise.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        FlaskPlayerData oldData = FlaskPlayerCapability.get(event.getOriginal());
        FlaskPlayerData newData = FlaskPlayerCapability.get(event.getEntityPlayer());
        if (oldData == null || newData == null) {
            return;
        }
        // The clone copies stored NBT, so the ticks since the last one-second flush must land
        // in the stack first or death would quietly rewind recharge progress.
        oldData.flushLiveProgress();
        newData.deserializeNBT(oldData.serializeNBT());
        if (!event.isWasDeath() || ConfigSnapshot.current().keepFlaskOnDeath()) {
            return;
        }
        ItemStack flask = newData.equippedFlask();
        if (flask.isEmpty()) {
            return;
        }
        boolean keepInventory = event.getOriginal().world.getGameRules()
                .getBoolean("keepInventory");
        if (keepInventory) {
            return;
        }
        newData.slot().setStackInSlot(0, ItemStack.EMPTY);
        EntityPlayer original = event.getOriginal();
        if (!original.world.isRemote) {
            original.world.spawnEntity(new EntityItem(original.world,
                    original.posX, original.posY, original.posZ, flask));
            Diagnostics.slotChanged(event.getEntityPlayer(), flask, ItemStack.EMPTY);
        }
    }
}
