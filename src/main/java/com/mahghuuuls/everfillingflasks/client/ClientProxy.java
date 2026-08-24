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
    public String useFlaskKeyName() {
        return FlaskKeys.useFlaskName();
    }

    @Override
    public boolean isLocalPlayerDrinking(net.minecraft.entity.player.EntityPlayer player) {
        return player == Minecraft.getMinecraft().player && ClientFlaskState.snapshot().drinking();
    }
}
