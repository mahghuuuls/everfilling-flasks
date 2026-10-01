package com.mahghuuuls.everfillingflasks.item;

import net.minecraft.item.Item;

/**
 * One built-in Flask Infusion. As thin as the Flasks: behavior lives in the registered
 * definition, numbers live in the config, and the item exists for registry identity. Its
 * tooltip is written by the core for every registered infusion alike
 * ({@code client.InfusionTooltips}), so the built-ins and add-on infusions cannot drift apart.
 */
public final class ItemInfusion extends Item {

    private final InfusionKind kind;

    public ItemInfusion(InfusionKind kind) {
        this.kind = kind;
        setRegistryName(kind.key());
        setTranslationKey("everfillingflasks." + kind.key());
    }

    public InfusionKind kind() {
        return kind;
    }
}
