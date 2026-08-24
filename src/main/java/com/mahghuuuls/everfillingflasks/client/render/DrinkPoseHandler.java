package com.mahghuuuls.everfillingflasks.client.render;

import com.mahghuuuls.everfillingflasks.client.ClientFlaskState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Raises a drinking player's arm in third person. Vanilla 1.12.2 has no drink pose for other
 * players, so the approved look is the potion-style raised item arm; the Flask itself is drawn
 * by the layer.
 *
 * <p>This listens to {@code RenderLivingEvent.Pre}, not {@code RenderPlayerEvent.Pre},
 * deliberately: the player renderer recomputes both arm poses from the actually held items
 * <em>after</em> the player event fires, so a pose written there is overwritten before it can
 * render. The living event fires after that recomputation.
 */
@SideOnly(Side.CLIENT)
public final class DrinkPoseHandler {

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<AbstractClientPlayer> event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (!ClientFlaskState.isDrinkingVisually(player)) {
            return;
        }
        RenderLivingBase<?> renderer = event.getRenderer();
        if (!(renderer.getMainModel() instanceof ModelBiped)) {
            return;
        }
        ModelBiped model = (ModelBiped) renderer.getMainModel();
        if (player.getPrimaryHand() == EnumHandSide.RIGHT) {
            model.rightArmPose = ModelBiped.ArmPose.ITEM;
        } else {
            model.leftArmPose = ModelBiped.ArmPose.ITEM;
        }
    }
}
