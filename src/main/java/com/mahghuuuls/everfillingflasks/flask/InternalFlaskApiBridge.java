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

    @Override
    protected void registerFlaskNow(net.minecraft.item.Item item,
                                    com.mahghuuuls.everfillingflasks.api.FlaskDefinition definition) {
        FlaskRegistry.register(item, definition);
    }

    @Override
    protected boolean isFlaskNow(net.minecraft.item.ItemStack stack) {
        return FlaskRegistry.isFlask(stack);
    }

    @Override
    protected com.mahghuuuls.everfillingflasks.api.FlaskDefinition definitionNow(
            net.minecraft.item.ItemStack stack) {
        return FlaskRegistry.definition(stack);
    }

    @Override
    protected com.mahghuuuls.everfillingflasks.api.FlaskSnapshot snapshotNow(
            net.minecraft.entity.player.EntityPlayer player) {
        if (player == null) {
            return com.mahghuuuls.everfillingflasks.player.FlaskSnapshots.empty();
        }
        if (player.world != null && player.world.isRemote) {
            // The client's answer is its mirror, and only for the local player; asked through
            // the proxy so this class stays loadable on a dedicated server.
            return com.mahghuuuls.everfillingflasks.EverfillingFlasksMod.proxy
                    .clientSnapshot(player);
        }
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            return com.mahghuuuls.everfillingflasks.player.FlaskSnapshots
                    .server((net.minecraft.entity.player.EntityPlayerMP) player);
        }
        return com.mahghuuuls.everfillingflasks.player.FlaskSnapshots.empty();
    }
}
