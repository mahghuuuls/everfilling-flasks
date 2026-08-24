package com.mahghuuuls.everfillingflasks.client.render;

import com.mahghuuuls.everfillingflasks.client.ClientFlaskState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Wraps vanilla's held-item layer and skips it while the player is visually drinking, so the
 * Flask from {@link ThirdPersonDrinkLayer} is the only item in the raised hand. Every other
 * frame delegates untouched, including {@code shouldCombineTextures}.
 *
 * <p>Installed by {@link HeldItemLayerInstaller}; owner-approved private access (2026-08-24)
 * because the layer list is not exposed. If installation fails the vanilla layer stays and the
 * only cost is the overlap this exists to remove.
 */
@SideOnly(Side.CLIENT)
public final class DrinkAwareHeldItemLayer implements LayerRenderer<AbstractClientPlayer> {

    @SuppressWarnings("rawtypes")
    private final LayerRenderer vanilla;

    public DrinkAwareHeldItemLayer(LayerRenderer<?> vanilla) {
        this.vanilla = vanilla;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (ClientFlaskState.isDrinkingVisually(player)) {
            return;
        }
        vanilla.doRenderLayer(player, limbSwing, limbSwingAmount, partialTicks, ageInTicks,
                netHeadYaw, headPitch, scale);
    }

    @Override
    public boolean shouldCombineTextures() {
        return vanilla.shouldCombineTextures();
    }
}
