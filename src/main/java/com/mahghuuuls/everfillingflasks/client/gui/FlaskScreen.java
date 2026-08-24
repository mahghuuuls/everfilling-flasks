package com.mahghuuuls.everfillingflasks.client.gui;

import com.mahghuuuls.everfillingflasks.client.ClientFlaskState;
import com.mahghuuuls.everfillingflasks.network.FlaskContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Flask screen: the Flask slot on the left, the nine-slot infusion grid in the middle,
 * and the potency line. The vanilla dispenser texture is a deliberate placeholder background:
 * its three-by-three happens to sit where the grid is, and the Flask slot's frame is one of
 * its cells redrawn at the slot's position; the approved scope leaves final artwork out.
 *
 * <p>The potency numbers are the server's, from the state message — the screen displays and
 * never computes them, so the display cannot disagree with the refusal rule.
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
        // The Flask slot's frame: one cell of the texture's own grid, redrawn where the slot
        // is, so the placeholder look stays consistent without any new art.
        drawTexturedModalRect(left + FlaskContainer.FLASK_SLOT_X - 1,
                top + FlaskContainer.FLASK_SLOT_Y - 1,
                FlaskContainer.GRID_X - 1, FlaskContainer.GRID_Y - 1, 18, 18);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("everfillingflasks.screen.title"), 8, 6, 0x404040);
        if (!ClientFlaskState.snapshot().hasFlask()) {
            return;
        }
        int used = ClientFlaskState.potencyUsed();
        int capacity = ClientFlaskState.potency();
        // The server's rule, called on the server's numbers: the screen computes nothing.
        boolean over = com.mahghuuuls.everfillingflasks.flask.FlaskMechanics
                .overCapacity(used, capacity);
        // The strip between the grid's bottom (y 71) and the inventory (y 84); nothing else
        // draws there, so the line can never overlap a slot.
        fontRenderer.drawString(I18n.format("everfillingflasks.screen.potency", used, capacity),
                8, 73, over ? 0xB02020 : 0x404040);
        if (over) {
            // Right-aligned on the title row, the other strip with guaranteed free space.
            String warning = I18n.format("everfillingflasks.screen.overCapacity");
            fontRenderer.drawString(warning,
                    xSize - 8 - fontRenderer.getStringWidth(warning), 6, 0xB02020);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
}
