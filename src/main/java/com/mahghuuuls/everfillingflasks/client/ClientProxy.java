package com.mahghuuuls.everfillingflasks.client;

import com.mahghuuuls.everfillingflasks.CommonProxy;
import com.mahghuuuls.everfillingflasks.client.hud.DefaultFlaskHud;
import com.mahghuuuls.everfillingflasks.network.FlaskStateMessage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

/**
 * Client-side initialization: the key binding and the Inventory Button Bar button. Item models
 * register earlier, from {@link ClientModels}, because model registration has its own event.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void registerSidedHandlers() {
        FlaskKeys.register();
        ButtonBarRegistration.register();
        MinecraftForge.EVENT_BUS.register(new ClientFlaskState());
        MinecraftForge.EVENT_BUS.register(new FlaskKeyHandler());
        MinecraftForge.EVENT_BUS.register(new DefaultFlaskHud());
        MinecraftForge.EVENT_BUS.register(
                new com.mahghuuuls.everfillingflasks.client.journal.JournalBuilder());
        MinecraftForge.EVENT_BUS.register(
                new com.mahghuuuls.everfillingflasks.client.hud.CastBarRenderer());
        MinecraftForge.EVENT_BUS.register(
                new com.mahghuuuls.everfillingflasks.client.render.FirstPersonDrinkRenderer());
        MinecraftForge.EVENT_BUS.register(
                new com.mahghuuuls.everfillingflasks.client.render.DrinkPoseHandler());
        // Both skin geometries have their own renderer; each gets its own layer instance,
        // installed once here at init so a resource reload cannot double them.
        for (net.minecraft.client.renderer.entity.RenderPlayer renderPlayer
                : Minecraft.getMinecraft().getRenderManager().getSkinMap().values()) {
            renderPlayer.addLayer(
                    new com.mahghuuuls.everfillingflasks.client.render.ThirdPersonDrinkLayer(
                            renderPlayer));
            com.mahghuuuls.everfillingflasks.client.render.HeldItemLayerInstaller
                    .install(renderPlayer);
        }
    }

    @Override
    public void handleFlaskState(final FlaskStateMessage message) {
        // Arrives on a netty thread; the mirror is read by render code on the main thread.
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientFlaskState.accept(message);
            }
        });
    }

    @Override
    public void handleDrinkVisual(final com.mahghuuuls.everfillingflasks.network.DrinkVisualMessage message) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientFlaskState.acceptVisual(message);
            }
        });
    }

    @Override
    public String useFlaskKeyName() {
        return FlaskKeys.useFlaskName();
    }

    @Override
    public boolean isLocalPlayerDrinking(net.minecraft.entity.player.EntityPlayer player) {
        return player == Minecraft.getMinecraft().player && ClientFlaskState.snapshot().drinking();
    }

    @Override
    public com.mahghuuuls.everfillingflasks.api.FlaskSnapshot clientSnapshot(
            net.minecraft.entity.player.EntityPlayer player) {
        if (player == Minecraft.getMinecraft().player) {
            return ClientFlaskState.snapshot();
        }
        return com.mahghuuuls.everfillingflasks.player.FlaskSnapshots.empty();
    }
}
