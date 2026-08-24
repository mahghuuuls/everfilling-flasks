package com.mahghuuuls.everfillingflasks.client.gui;

import com.mahghuuuls.everfillingflasks.network.FlaskContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Flask screen: one slot above the player's inventory. The vanilla dispenser texture is a
 * deliberate placeholder background whose center slot happens to sit where the Flask slot is;
 * the approved scope leaves final screen artwork out, and only the middle slot of the texture's
 * three-by-three grid has a live slot behind it.
 */
@SideOnly(Side.CLIENT)
public final class FlaskScreen extends GuiContainer {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("minecraft", "textures/gui/container/dispenser.png");

    public FlaskScreen(FlaskContainer container) {
        super(container);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawDefaultBackground();
        mc.getTextureManager().bindTexture(BACKGROUND);
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        drawTexturedModalRect(left, top, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("everfillingflasks.screen.title"), 8, 6, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
}
