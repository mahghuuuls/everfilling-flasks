package com.mahghuuuls.everfillingflasks.devfixtures;

import net.minecraft.item.Item;

/**
 * The fixture add-on's own Flask item: proof that a third-party item becomes a Flask by
 * registration alone. Deliberately a plain {@link Item} subclass with nothing overridden —
 * the core must never require inheritance or casts.
 */
public final class FixtureManaFlask extends Item {

    public FixtureManaFlask() {
        setMaxStackSize(1);
        setRegistryName(DevFixturesMod.MOD_ID, "mana_flask");
        setTranslationKey("everfillingflasksdev.mana_flask");
    }
}
