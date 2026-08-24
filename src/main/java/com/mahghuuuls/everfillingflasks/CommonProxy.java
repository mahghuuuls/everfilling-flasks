package com.mahghuuuls.everfillingflasks;

/**
 * Sided initialization boundary. The server build of the mod runs exactly this; the client
 * overrides {@link #registerSidedHandlers()} to add its key binding, bar button, and rendering.
 */
public class CommonProxy {

    public void registerSidedHandlers() {
    }

    /**
     * The Use Flask key's current display name, for tooltips. The server has no key bindings
     * and no tooltips; this default exists so shared code never names a client class.
     */
    public String useFlaskKeyName() {
        return "?";
    }

    /**
     * Receives a Flask state message. Only a client has anywhere to put one; the server
     * registers the message type but never receives it, so this default is unreachable.
     */
    public void handleFlaskState(com.mahghuuuls.everfillingflasks.network.FlaskStateMessage message) {
    }

    /**
     * Whether this player is the local client player mid-drink, for the client halves of the
     * action guards. The server overrides nothing here: its guards read the capability, and
     * this default keeps client state out of every server code path.
     */
    public boolean isLocalPlayerDrinking(net.minecraft.entity.player.EntityPlayer player) {
        return false;
    }
}
