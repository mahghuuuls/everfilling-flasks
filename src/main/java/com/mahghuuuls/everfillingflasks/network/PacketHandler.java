package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.Tags;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The mod's one network channel. Message ids are assigned by registration order here and must
 * stay in this file so client and server cannot disagree about them.
 */
public final class PacketHandler {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    private PacketHandler() {
    }

    public static void register() {
        int nextId = 0;
        CHANNEL.registerMessage(OpenFlaskScreenMessage.Handler.class,
                OpenFlaskScreenMessage.class, nextId++, Side.SERVER);
    }
}
