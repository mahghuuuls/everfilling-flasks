package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskModifierSource;
import com.mahghuuuls.everfillingflasks.api.internal.FlaskApiBridge;

/** Binds the public facade to the internal registries; installed once at pre-initialization. */
public final class InternalFlaskApiBridge extends FlaskApiBridge {

    public static void install() {
        FlaskApiBridge.bind(new InternalFlaskApiBridge());
    }

    private InternalFlaskApiBridge() {
    }

    @Override
    protected void registerModifierSourceNow(FlaskModifierSource source) {
        ModifierRegistry.register(source);
    }

    @Override
    protected void registerIngredientNow(net.minecraft.item.Item item,
                                         com.mahghuuuls.everfillingflasks.api.IngredientDefinition definition) {
        IngredientRegistry.register(item, definition);
    }
}
