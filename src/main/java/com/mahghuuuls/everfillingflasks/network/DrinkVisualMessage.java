package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A player starting or stopping a drink, broadcast to everyone who can see them, so third-person
 * rendering works for watchers who never receive that player's state. Carries the Flask stack
 * for display identity: a watcher has no other way to know which bottle to draw.
 *
 * <p>Presentation only. Nothing here feeds gameplay, and the handler delegates through the
 * proxy so the class stays loadable on a dedicated server.
 */
public final class DrinkVisualMessage implements IMessage {

    private int entityId;
    private boolean drinking;
    private int drinkTicks;
    private ItemStack flask = ItemStack.EMPTY;

    public DrinkVisualMessage() {
    }

    public DrinkVisualMessage(int entityId, boolean drinking, int drinkTicks, ItemStack flask) {
        this.entityId = entityId;
        this.drinking = drinking;
        this.drinkTicks = drinkTicks;
        this.flask = flask == null ? ItemStack.EMPTY : flask;
    }

    public int entityId() {
        return entityId;
    }

    public boolean drinking() {
        return drinking;
    }

    public int drinkTicks() {
        return drinkTicks;
    }

    public ItemStack flask() {
        return flask;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        drinking = buf.readBoolean();
        drinkTicks = buf.readInt();
        flask = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(drinking);
        buf.writeInt(drinkTicks);
        ByteBufUtils.writeItemStack(buf, flask);
    }

    public static final class Handler implements IMessageHandler<DrinkVisualMessage, IMessage> {

        @Override
        public IMessage onMessage(DrinkVisualMessage message, MessageContext ctx) {
            EverfillingFlasksMod.proxy.handleDrinkVisual(message);
            return null;
        }
    }
}
