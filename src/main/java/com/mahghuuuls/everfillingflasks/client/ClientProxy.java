package com.mahghuuuls.everfillingflasks.client;

import com.mahghuuuls.everfillingflasks.CommonProxy;

/**
 * Client-side initialization: the key binding and the Inventory Button Bar button. Item models
 * register earlier, from {@link ClientModels}, because model registration has its own event.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void registerSidedHandlers() {
        FlaskKeys.register();
        ButtonBarRegistration.register();
    }

    @Override
    public String useFlaskKeyName() {
        return FlaskKeys.useFlaskName();
    }
}
