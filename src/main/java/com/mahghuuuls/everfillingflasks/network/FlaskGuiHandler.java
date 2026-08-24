package com.mahghuuuls.everfillingflasks.network;

import com.mahghuuuls.everfillingflasks.client.gui.FlaskScreen;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerCapability;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/**
 * Builds the Flask screen's container on the server and its GUI on the client. The client
 * method names a client-only class; FML calls it on the client alone, which is the standard
 * IGuiHandler shape.
 */
public final class FlaskGuiHandler implements IGuiHandler {

    public static final int FLASK_SCREEN = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != FLASK_SCREEN) {
            return null;
        }
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data == null) {
            return null;
        }
        return new FlaskContainer(player.inventory, data);
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != FLASK_SCREEN) {
            return null;
        }
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data == null) {
            return null;
        }
        return new FlaskScreen(new FlaskContainer(player.inventory, data));
    }
}
