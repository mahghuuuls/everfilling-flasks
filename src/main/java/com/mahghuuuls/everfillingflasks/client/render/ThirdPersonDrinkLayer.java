package com.mahghuuuls.everfillingflasks.client.render;

import com.mahghuuuls.everfillingflasks.client.ClientFlaskState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws the Flask in a drinking player's raised hand for everyone watching. The stack comes
 * from the drink-visual broadcast, never from the watcher's own state, so any number of players
 * can drink at once and each shows their own bottle.
 *
 * <p>The transform follows the arm the pose handler raised: anchored with
 * {@code postRenderArm}, then the vanilla held-item offsets for a hand raised in the potion
 * pose.
 */
@SideOnly(Side.CLIENT)
public final class ThirdPersonDrinkLayer implements LayerRenderer<AbstractClientPlayer> {

    private final RenderPlayer renderer;
    private boolean failedThisSession;

    public ThirdPersonDrinkLayer(RenderPlayer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (failedThisSession) {
            return;
        }
        ItemStack flask = ClientFlaskState.visualFlask(player);
        if (flask.isEmpty()) {
            return;
        }
        EnumHandSide side = player.getPrimaryHand();
        GlStateManager.pushMatrix();
        try {
            if (player.isSneaking()) {
                // The vanilla held-item layer applies the same crouch offset before anchoring
                // to the hand; without it a sneaking drinker's bottle floats above the fist.
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }
            renderer.getMainModel().postRenderArm(0.0625F, side);
            GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            boolean left = side == EnumHandSide.LEFT;
            GlStateManager.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            Minecraft.getMinecraft().getRenderItem().renderItem(flask, player,
                    left ? ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND
                            : ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, left);
        } catch (Throwable failure) {
            failedThisSession = true;
            com.mahghuuuls.everfillingflasks.EverfillingFlasksMod.LOGGER.error(
                    "Third-person drink rendering failed and is disabled for this session",
                    failure);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
