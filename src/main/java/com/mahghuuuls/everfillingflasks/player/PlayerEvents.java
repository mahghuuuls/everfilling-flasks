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

/**
 * Lifecycle wiring for the player capability: attach, carry across death and the End return,
 * and the login-time starting-Flask grant. State sync to the client joins in a later slice.
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
