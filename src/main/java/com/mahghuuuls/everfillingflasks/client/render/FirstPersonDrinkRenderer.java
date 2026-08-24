package com.mahghuuuls.everfillingflasks.client.render;

import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import com.mahghuuuls.everfillingflasks.client.ClientFlaskState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The first-person drinking view: while the local player drinks, the main hand shows the Flask
 * being raised and tilted like a vanilla drink, whatever the hand actually holds. Purely visual:
 * the real held item and the Flask slot never change, and everything here reads the synced
 * mirror only.
 *
 * <p>Isolated like every renderer: a failure logs once and the vanilla hand comes back, because
 * a broken pose must never take rendering down.
 */
@SideOnly(Side.CLIENT)
public final class FirstPersonDrinkRenderer {

    private boolean failedThisSession;

    @SubscribeEvent
    public void onRenderHand(RenderSpecificHandEvent event) {
        if (event.getHand() != EnumHand.MAIN_HAND || failedThisSession) {
            return;
        }
        FlaskSnapshot state = ClientFlaskState.snapshot();
        if (!state.drinking() || state.flask().isEmpty()) {
            return;
        }
        event.setCanceled(true);
        try {
            renderDrinkingFlask(state, event.getPartialTicks(), event.getEquipProgress());
        } catch (Throwable failure) {
            failedThisSession = true;
            com.mahghuuuls.everfillingflasks.EverfillingFlasksMod.LOGGER.error(
                    "First-person drink rendering failed and is disabled for this session;"
                            + " the vanilla hand is shown instead", failure);
        }
    }

    /**
     * The vanilla drink pose, reproduced: the raise-to-face motion and gulp bob from
     * {@link DrinkTransforms} first, then the side-hand base position inside that raised frame,
     * then the item itself. The angles and offsets mirror the private vanilla eat transform.
     */
    private void renderDrinkingFlask(FlaskSnapshot state, float partialTicks, float equipProgress) {
        Minecraft mc = Minecraft.getMinecraft();
        EnumHandSide side = mc.player.getPrimaryHand();
        int mirror = side == EnumHandSide.RIGHT ? 1 : -1;

        float progress = Math.min(1.0F,
                (state.drinkProgressTicks() + partialTicks) / Math.max(1, state.drinkTicks()));
        float remainingTicks = Math.max(0.0F,
                state.drinkTicks() - (state.drinkProgressTicks() + partialTicks));
        float raise = DrinkTransforms.raise(progress);
        float bob = DrinkTransforms.bob(progress, remainingTicks);

        ItemStack flask = state.flask();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableRescaleNormal();
            // Vanilla's composition order, which is what swings the bottle up to the face: the
            // eat transforms come first, so the side-hand offset applied after them is expressed
            // inside the rotated frame. Reversing this order strands the bottle at the hip.
            GlStateManager.translate(0.0F, bob, 0.0F);
            GlStateManager.translate(raise * 0.6F * mirror, raise * -0.5F, 0.0F);
            GlStateManager.rotate(mirror * raise * 90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(raise * 10.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(mirror * raise * 30.0F, 0.0F, 0.0F, 1.0F);
            // The side-hand position, sliding with equip progress exactly like vanilla.
            GlStateManager.translate(mirror * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);

            mc.getItemRenderer().renderItemSide(mc.player, flask,
                    side == EnumHandSide.RIGHT
                            ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
                            : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND,
                    side != EnumHandSide.RIGHT);
        } finally {
            // No disableRescaleNormal here: vanilla enables it once around BOTH hands and
            // disables it after the off-hand, which renders after this event returns.
            GlStateManager.popMatrix();
        }
    }
}
