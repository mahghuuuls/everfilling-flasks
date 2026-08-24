package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.player.DrinkController;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * The Use Flask key changing state, and nothing else. The client reports edges; every decision
 * about what a press or release means happens on the server, so a modified client can at most
 * hold its own key.
 */
public final class FlaskKeyMessage implements IMessage {

    /** Why a release happened, for the diagnostics line only; the server acts identically. */
    public static final byte RELEASE_KEY = 0;
    public static final byte RELEASE_SCREEN = 1;

    private boolean pressed;
    private byte releaseCause = RELEASE_KEY;

    public FlaskKeyMessage() {
    }

    public FlaskKeyMessage(boolean pressed, byte releaseCause) {
        this.pressed = pressed;
        this.releaseCause = releaseCause;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pressed = buf.readBoolean();
        releaseCause = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(pressed);
        buf.writeByte(releaseCause);
    }

    public static final class Handler implements IMessageHandler<FlaskKeyMessage, IMessage> {

        @Override
        public IMessage onMessage(FlaskKeyMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            boolean pressed = message.pressed;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (pressed) {
                        DrinkController.keyPressed(player);
                    } else {
                        // The cause names the log line and nothing else: a hostile value can
                        // only mislabel its own diagnostics entry.
                        DrinkController.keyReleased(player,
                                message.releaseCause == RELEASE_SCREEN
                                        ? "screen opened" : "key released");
                    }
                }
            });
            return null;
        }
    }
}
