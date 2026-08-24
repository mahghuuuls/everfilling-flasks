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

    /** A start transition; no drink has ended. */
    public static final byte OUTCOME_NONE = 0;
    /** The drink ran its full duration; the charge was spent. */
    public static final byte OUTCOME_COMPLETED = 1;
    /** The drink ended early for any reason: release, hit, swap, death. */
    public static final byte OUTCOME_INTERRUPTED = 2;

    private int entityId;
    private boolean drinking;
    private int drinkTicks;
    private ItemStack flask = ItemStack.EMPTY;
    private byte outcome = OUTCOME_NONE;

    public DrinkVisualMessage() {
    }

    public DrinkVisualMessage(int entityId, boolean drinking, int drinkTicks, ItemStack flask,
                              byte outcome) {
        this.entityId = entityId;
        this.drinking = drinking;
        this.drinkTicks = drinkTicks;
        this.flask = flask == null ? ItemStack.EMPTY : flask;
        this.outcome = outcome;
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

    /** How the drink ended, display-only; {@link #OUTCOME_NONE} on a start transition. */
    public byte outcome() {
        return outcome;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        drinking = buf.readBoolean();
        drinkTicks = buf.readInt();
        flask = ByteBufUtils.readItemStack(buf);
        outcome = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(drinking);
        buf.writeInt(drinkTicks);
        ByteBufUtils.writeItemStack(buf, flask);
        buf.writeByte(outcome);
    }

    public static final class Handler implements IMessageHandler<DrinkVisualMessage, IMessage> {

        @Override
        public IMessage onMessage(DrinkVisualMessage message, MessageContext ctx) {
            EverfillingFlasksMod.proxy.handleDrinkVisual(message);
            return null;
        }
    }
}
