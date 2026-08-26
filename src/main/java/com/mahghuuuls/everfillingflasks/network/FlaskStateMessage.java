package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * The server telling a player their own Flask state: charges, effective maximum, recharge
 * progress and duration, the pause flag, drinking progress, and the effective hit threshold.
 * The client stores it and interpolates; nothing in it flows back.
 *
 * <p>The handler delegates to the sided proxy so this class stays loadable on a dedicated
 * server, where the message is registered but never received.
 */
public final class FlaskStateMessage implements IMessage {

    private boolean hasFlask;
    private ItemStack flask = ItemStack.EMPTY;
    private int charges;
    private int maxCharges;
    private int progressTicks;
    private int rechargeTicks;
    private boolean rechargePaused;
    private boolean drinking;
    private int drinkProgressTicks;
    private int drinkTicks;
    private float hitThreshold;
    private int potencyUsed;
    private int potency;

    public FlaskStateMessage() {
    }

    public FlaskStateMessage(boolean hasFlask, ItemStack flask, int charges, int maxCharges,
                             int progressTicks, int rechargeTicks, boolean rechargePaused,
                             boolean drinking, int drinkProgressTicks, int drinkTicks,
                             float hitThreshold, int potencyUsed, int potency) {
        this.drinking = drinking;
        this.drinkProgressTicks = drinkProgressTicks;
        this.drinkTicks = drinkTicks;
        this.hasFlask = hasFlask;
        this.flask = flask == null ? ItemStack.EMPTY : flask;
        this.charges = charges;
        this.maxCharges = maxCharges;
        this.progressTicks = progressTicks;
        this.rechargeTicks = rechargeTicks;
        this.rechargePaused = rechargePaused;
        this.hitThreshold = hitThreshold;
        this.potencyUsed = potencyUsed;
        this.potency = potency;
    }

    public static FlaskStateMessage empty() {
        return new FlaskStateMessage(false, ItemStack.EMPTY, 0, 0, 0, 1, false, false, 0, 1,
                0.0F, 0, 0);
    }

    public boolean hasFlask() {
        return hasFlask;
    }

    /** The equipped stack as the server last saw it; display identity only, never authority. */
    public ItemStack flask() {
        return flask;
    }

    public int charges() {
        return charges;
    }

    public int maxCharges() {
        return maxCharges;
    }

    public int progressTicks() {
        return progressTicks;
    }

    public int rechargeTicks() {
        return rechargeTicks;
    }

    public boolean rechargePaused() {
        return rechargePaused;
    }

    public boolean drinking() {
        return drinking;
    }

    public int drinkProgressTicks() {
        return drinkProgressTicks;
    }

    public int drinkTicks() {
        return drinkTicks;
    }

    public float hitThreshold() {
        return hitThreshold;
    }

    /** Summed potency costs of the placed infusions, for the screen's potency display. */
    public int potencyUsed() {
        return potencyUsed;
    }

    /** The equipped Flask's potency budget. */
    public int potency() {
        return potency;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        hasFlask = buf.readBoolean();
        flask = ByteBufUtils.readItemStack(buf);
        charges = buf.readInt();
        maxCharges = buf.readInt();
        progressTicks = buf.readInt();
        rechargeTicks = buf.readInt();
        rechargePaused = buf.readBoolean();
        drinking = buf.readBoolean();
        drinkProgressTicks = buf.readInt();
        drinkTicks = buf.readInt();
        hitThreshold = buf.readFloat();
        potencyUsed = buf.readInt();
        potency = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(hasFlask);
        ByteBufUtils.writeItemStack(buf, flask);
        buf.writeInt(charges);
        buf.writeInt(maxCharges);
        buf.writeInt(progressTicks);
        buf.writeInt(rechargeTicks);
        buf.writeBoolean(rechargePaused);
        buf.writeBoolean(drinking);
        buf.writeInt(drinkProgressTicks);
        buf.writeInt(drinkTicks);
        buf.writeFloat(hitThreshold);
        buf.writeInt(potencyUsed);
        buf.writeInt(potency);
    }

    public static final class Handler implements IMessageHandler<FlaskStateMessage, IMessage> {

        @Override
        public IMessage onMessage(FlaskStateMessage message, MessageContext ctx) {
            EverfillingFlasksMod.proxy.handleFlaskState(message);
            return null;
        }
    }
}
