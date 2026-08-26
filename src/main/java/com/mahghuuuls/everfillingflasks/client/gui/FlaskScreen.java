package com.mahghuuuls.everfillingflasks.client.gui;

import com.mahghuuuls.everfillingflasks.client.ClientFlaskState;
import com.mahghuuuls.everfillingflasks.client.journal.JournalBridge;
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
 * The Flask screen, in the owner's 2026-08-25 shape: the Flask slot on the left, the six-slot
 * ingredient row beside it on the right, and the potency shown as pips under the row —
 * one pip per potency point in rows of ten (up to thirty), filled per used point, all red
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

    /**
     * Pip geometry: rows of ten, a new row per ten potency, at most three rows (the owner's
     * bound: display must hold up to 30); above it, numbers take over.
     */
    private static final int PIP_LIMIT = 30;
    private static final int PIPS_PER_ROW = 10;
    private static final int PIP_SIZE = 7;
    private static final int PIP_STEP = 9;
    private static final int PIP_ROW_STEP = 8;
    private static final int PIP_Y = 50;

    private static final int PIP_BORDER = 0xFF373737;
    private static final int PIP_EMPTY = 0xFF8B8B8B;
    private static final int PIP_FILLED = 0xFFE8A33C;
    private static final int PIP_OVER = 0xFFD03030;
    private static final int TEXT_OVER = 0xB02020;

    /** Under the Flask slot, in the strip the pips never reach (they start at the row's x). */
    private static final int JOURNAL_X = FlaskContainer.FLASK_SLOT_X - 1;
    private static final int JOURNAL_Y = PIP_Y - 1;
    private static final int JOURNAL_ID = 0;

    public FlaskScreen(FlaskContainer container) {
        super(container);
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new JournalButton(JOURNAL_ID, guiLeft + JOURNAL_X, guiTop + JOURNAL_Y));
    }

    @Override
    protected void actionPerformed(net.minecraft.client.gui.GuiButton button) {
        if (button.id == JOURNAL_ID) {
            // Closed properly first. The journal is a screen of its own, and simply displaying
            // it would leave the server believing this container is still open and would settle
            // a stack held on the cursor on the client alone.
            mc.player.closeScreen();
            JournalBridge.open();
        }
    }

    /**
     * The journal control: the book itself drawn in one of the background texture's slot frames,
     * so the button matches the screen and still costs no new art. It is always present, because
     * the journal is reference material rather than something the Flask state gates (REQ-035).
     */
    @SideOnly(Side.CLIENT)
    private static final class JournalButton extends net.minecraft.client.gui.GuiButton {

        private static final int SIZE = 18;
        private static final int HOVER_TINT = 0x80FFFFFF;

        JournalButton(int id, int x, int y) {
            super(id, x, y, SIZE, SIZE, "");
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY,
                float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(BACKGROUND);
            drawTexturedModalRect(x, y, FRAME_U, FRAME_V, SIZE, SIZE);

            net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(JournalBridge.buttonIcon(), x + 1, y + 1);
            net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();

            if (hovered) {
                Gui.drawRect(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, HOVER_TINT);
            }
        }
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
        // The Flask slot's frame always; the row's frames only while a Flask is in, matching
        // the slots' own isEnabled gate, so the empty screen shows just the one slot.
        drawTexturedModalRect(left + FlaskContainer.FLASK_SLOT_X - 1,
                top + FlaskContainer.FLASK_SLOT_Y - 1, FRAME_U, FRAME_V, 18, 18);
        if (((FlaskContainer) inventorySlots).flaskEquipped()) {
            for (int column = 0; column < FlaskStackState.GRID_SIZE; column++) {
                drawTexturedModalRect(left + FlaskContainer.GRID_X - 1 + column * 18,
                        top + FlaskContainer.GRID_Y - 1, FRAME_U, FRAME_V, 18, 18);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("everfillingflasks.screen.title"), 8, 6, 0x404040);
        // Both gates on purpose: the container's slot content controls what exists (same as
        // the slots' isEnabled), the state message supplies the numbers to show.
        if (!((FlaskContainer) inventorySlots).flaskEquipped()
                || !ClientFlaskState.snapshot().hasFlask()) {
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

    /**
     * One pip per potency point, in rows of ten; filled per used point; every pip red when
     * over capacity.
     */
    private void drawPips(int used, int capacity, boolean over) {
        for (int i = 0; i < capacity; i++) {
            int x = FlaskContainer.GRID_X + (i % PIPS_PER_ROW) * PIP_STEP;
            int y = PIP_Y + (i / PIPS_PER_ROW) * PIP_ROW_STEP;
            int inner = over ? PIP_OVER : i < used ? PIP_FILLED : PIP_EMPTY;
            Gui.drawRect(x, y, x + PIP_SIZE, y + PIP_SIZE, PIP_BORDER);
            Gui.drawRect(x + 1, y + 1, x + PIP_SIZE - 1, y + PIP_SIZE - 1, inner);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        // The journal button is a plain button, not a slot, so its label is drawn here rather
        // than by the container's own hover handling.
        // Not while a stack is being carried, the way vanilla suppresses slot tooltips: the
        // label would otherwise sit behind the dragged item.
        if (mc.player.inventory.getItemStack().isEmpty()
                && mouseX >= guiLeft + JOURNAL_X && mouseX < guiLeft + JOURNAL_X + 18
                && mouseY >= guiTop + JOURNAL_Y && mouseY < guiTop + JOURNAL_Y + 18) {
            drawHoveringText(
                    java.util.Collections.singletonList(
                            I18n.format("everfillingflasks.journal.button")),
                    mouseX, mouseY);
        }
    }
}
