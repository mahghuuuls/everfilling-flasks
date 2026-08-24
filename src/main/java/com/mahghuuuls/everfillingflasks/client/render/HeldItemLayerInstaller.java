package com.mahghuuuls.everfillingflasks.client.render;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * Replaces the vanilla held-item layer on a player renderer with {@link DrinkAwareHeldItemLayer}
 * so the real held item is hidden while that player drinks.
 *
 * <p>The layer list is {@code RenderLivingBase.layerRenderers}, a protected field with no
 * accessor beyond add, so this is the project's one reflective access, owner-approved
 * (2026-08-24). Both the development and the SRG field name are given, which is how the same
 * jar works in the development workspace and in a released game. Any failure is caught here:
 * the vanilla layer then stays in place, everything else keeps working, and the only loss is
 * the held-item overlap the wrapper would have removed.
 *
 * <p>Compatibility note: after installation another mod scanning the list for
 * {@code instanceof LayerHeldItem} no longer finds one, and a mod that replaces the layer after
 * client init silently replaces the wrapper too. Both degrade to the cosmetic overlap, never a
 * crash.
 */
@SideOnly(Side.CLIENT)
public final class HeldItemLayerInstaller {

    private HeldItemLayerInstaller() {
    }

    /** Called once per skin-map renderer at client init, never on resource reload. */
    @SuppressWarnings("rawtypes")
    public static void install(RenderPlayer renderPlayer) {
        try {
            List layers = ObfuscationReflectionHelper.getPrivateValue(
                    RenderLivingBase.class, renderPlayer, "field_177097_h", "layerRenderers");
            boolean replaced = false;
            for (int i = 0; i < layers.size(); i++) {
                Object layer = layers.get(i);
                if (layer instanceof LayerHeldItem) {
                    layers.set(i, new DrinkAwareHeldItemLayer((LayerRenderer<?>) layer));
                    replaced = true;
                }
            }
            if (!replaced) {
                EverfillingFlasksMod.LOGGER.warn(
                        "No vanilla held-item layer found to wrap; a drinking player's held"
                                + " item will overlap the Flask in third person");
            }
        } catch (Throwable failure) {
            EverfillingFlasksMod.LOGGER.warn(
                    "Could not wrap the vanilla held-item layer; a drinking player's held item"
                            + " will overlap the Flask in third person", failure);
        }
    }
}
