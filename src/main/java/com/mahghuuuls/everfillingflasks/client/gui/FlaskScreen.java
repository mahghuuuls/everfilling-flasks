package com.mahghuuuls.everfillingflasks.client.gui;

import com.mahghuuuls.everfillingflasks.client.ClientFlaskState;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import com.mahghuuuls.everfillingflasks.network.FlaskContainer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Flask screen, in the owner's 2026-08-25 shape: the Flask slot on the left, six
 * ingredient slots in one row (a three-by-three read as a crafting table), and the potency
 * shown as pips under the row — one pip per potency point, filled per used point, all red
 * plus a warning when over capacity. A configured potency too large for pips falls back to
 * plain numbers.
 *
 * <p>Still zero new art: the vanilla dispenser texture is the placeholder background, its
 * unused three-by-three painted over in panel grey, and every slot frame is one of its cells
 * redrawn. The potency numbers are the server's, from the state message — the screen displays
 * and never computes them, so the display cannot disagree with the refusal rule.
 */
@SideOnly(Side.CLIENT)
public final class FlaskScreen extends GuiContainer {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("minecraft", "textures/gui/container/dispenser.png");

    /** The dispenser texture's own top-left slot cell, reused as the frame stamp. */
    private static final int FRAME_U = 61;
    private static final int FRAME_V = 16;

    /** The vanilla container panel grey, for painting over the texture's unused grid. */
    private static final int PANEL_GREY = 0xFFC6C6C6;

    /** Pip geometry: at most this many pips fit the row; above it, numbers take over. */
    private static final int PIP_LIMIT = 12;
    private static final int PIP_SIZE = 7;
    private static final int PIP_STEP = 9;
    private static final int PIP_Y = 48;

    private static final int PIP_BORDER = 0xFF373737;
    private static final int PIP_EMPTY = 0xFF8B8B8B;
    private static final int PIP_FILLED = 0xFFE8A33C;
    private static final int PIP_OVER = 0xFFD03030;
    private static final int TEXT_OVER = 0xB02020;

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
        // The texture's three-by-three is not where our slots are; paint it out.
        Gui.drawRect(left + 61, top + 15, left + 115, top + 71, PANEL_GREY);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        // The Flask slot's frame, then the six-slot row's frames.
        drawTexturedModalRect(left + FlaskContainer.FLASK_SLOT_X - 1,
                top + FlaskContainer.FLASK_SLOT_Y - 1, FRAME_U, FRAME_V, 18, 18);
        for (int column = 0; column < FlaskStackState.GRID_SIZE; column++) {
            drawTexturedModalRect(left + FlaskContainer.GRID_X - 1 + column * 18,
                    top + FlaskContainer.GRID_Y - 1, FRAME_U, FRAME_V, 18, 18);
        }
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
        if (capacity >= 1 && capacity <= PIP_LIMIT) {
            drawPips(used, capacity, over);
        } else {
            // A potency outside the pip range (0, or a pack author's huge value): numbers.
            fontRenderer.drawString(used + " / " + capacity, FlaskContainer.GRID_X, PIP_Y,
                    over ? TEXT_OVER : 0x404040);
        }
        if (over) {
            String warning = I18n.format("everfillingflasks.screen.overCapacity");
            fontRenderer.drawString(warning,
                    xSize - 8 - fontRenderer.getStringWidth(warning), 6, TEXT_OVER);
        }
    }

    /** One pip per potency point; filled per used point; every pip red when over capacity. */
    private void drawPips(int used, int capacity, boolean over) {
        for (int i = 0; i < capacity; i++) {
            int x = FlaskContainer.GRID_X + i * PIP_STEP;
            int inner = over ? PIP_OVER : i < used ? PIP_FILLED : PIP_EMPTY;
            Gui.drawRect(x, PIP_Y, x + PIP_SIZE, PIP_Y + PIP_SIZE, PIP_BORDER);
            Gui.drawRect(x + 1, PIP_Y + 1, x + PIP_SIZE - 1, PIP_Y + PIP_SIZE - 1, inner);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
}
