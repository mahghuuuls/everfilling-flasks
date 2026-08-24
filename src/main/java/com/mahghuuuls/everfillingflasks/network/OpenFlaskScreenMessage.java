package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * The client asking for the Flask screen. Carries nothing: the server decides what the screen
 * shows from its own state, so there is nothing a modified client could inject here.
 */
public final class OpenFlaskScreenMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static final class Handler
            implements IMessageHandler<OpenFlaskScreenMessage, IMessage> {

        @Override
        public IMessage onMessage(OpenFlaskScreenMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            // Network threads may not touch world state; the open happens on the server thread.
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    player.openGui(EverfillingFlasksMod.instance(), FlaskGuiHandler.FLASK_SCREEN,
                            player.world, 0, 0, 0);
                }
            });
            return null;
        }
    }
}
