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
}
