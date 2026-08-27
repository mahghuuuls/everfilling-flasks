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
 * The Flask screen: the Flask slot on the left, the infusion grid beside it in rows of six, and
 * the potency shown as pips under whatever the grid occupies — one pip per potency point in
 * rows of ten, filled per used point, all red plus a warning when over capacity. A potency too
 * large for the space left below the grid falls back to plain numbers.
 *
 * <p>The panel is still the vanilla dispenser texture with its unused three-by-three painted
 * over, and every slot frame is one of its cells redrawn; only the journal button carries art
 * of ours. The potency numbers are the server's, from the state message — the screen displays
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
     * Pip geometry: rows of ten, a new row per ten potency, as many rows as fit between the
     * grid and the player inventory; above that, numbers take over.
     */
    private static final int PIPS_PER_ROW = 10;
    private static final int PIP_SIZE = 7;
    private static final int PIP_STEP = 9;
    private static final int PIP_ROW_STEP = 8;

    /** Just under the infusion grid, wherever the grid happens to end. */
    private static final int PIP_GAP = 4;

    /** The player's own inventory starts here; the pips must stay above it. */
    private static final int INVENTORY_TOP = 84;

    private static final int PIP_BORDER = 0xFF373737;
    private static final int PIP_EMPTY = 0xFF8B8B8B;
    private static final int PIP_FILLED = 0xFFE8A33C;
    private static final int PIP_OVER = 0xFFD03030;
    private static final int TEXT_OVER = 0xB02020;

    /**
     * The journal control: a real button with a book on it, not an item in a slot. The face is
     * vanilla's own button art from the widget sheet, so the raised edge and the hover highlight
     * are the ones players already know; 1.12.2 has no dedicated recipe-book button to borrow.
     * The book on it is ours, drawn in the shape and shading a Minecraft book has.
     */
    private static final ResourceLocation WIDGETS =
            new ResourceLocation("minecraft", "textures/gui/widgets.png");
    private static final int JOURNAL_W = 20;
    private static final int JOURNAL_H = 20;
    /** The widget sheet's button row, and the highlighted row one button below it. */
    private static final int BUTTON_V = 66;
    private static final int BUTTON_HOVER_V = 86;
    /** The sheet's button is 200 wide; its two ends are drawn to make a short one. */
    private static final int BUTTON_SHEET_W = 200;

    /** The journal's own book, drawn on the button face. */
    private static final ResourceLocation JOURNAL_ICON =
            new ResourceLocation(com.mahghuuuls.everfillingflasks.Tags.MOD_ID,
                    "textures/gui/journal_button.png");
    private static final int ICON = 16;

    /**
     * Under the Flask slot, in the left column. It stays put whatever the grid does, because
     * the grid and the potency display live to the right of it.
     */
    private static final int JOURNAL_X = FlaskContainer.FLASK_SLOT_X - 2;
    private static final int JOURNAL_Y = FlaskContainer.FLASK_SLOT_Y + 21;
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
     * A short vanilla button with the journal drawn on its face.
     *
     * <p>The widget sheet only holds a full-width button, so its left and right ends are drawn
     * side by side to make a small square one; that is how vanilla itself builds narrow buttons.
     */
    @SideOnly(Side.CLIENT)
    private static final class JournalButton extends net.minecraft.client.gui.GuiButton {

        JournalButton(int id, int x, int y) {
            super(id, x, y, JOURNAL_W, JOURNAL_H, "");
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY,
                float partialTicks) {
            if (!visible) {
                return;
            }
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(WIDGETS);
            int v = hovered ? BUTTON_HOVER_V : BUTTON_V;
            int half = JOURNAL_W / 2;
            drawTexturedModalRect(x, y, 0, v, half, JOURNAL_H);
            drawTexturedModalRect(x + half, y, BUTTON_SHEET_W - half, v, half, JOURNAL_H);

            mc.getTextureManager().bindTexture(JOURNAL_ICON);
            drawModalRectWithCustomSizedTexture(x + 2, y + 2, 0, 0, ICON, ICON, ICON, ICON);
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
        // One frame per slot the equipped Flask actually has, in rows of six, matching the
        // slots' own enabling so a frame never sits under nothing.
        int slots = ((FlaskContainer) inventorySlots).activeInfusionSlots();
        for (int index = 0; index < slots; index++) {
            drawTexturedModalRect(
                    left + FlaskContainer.GRID_X - 1
                            + (index % FlaskContainer.GRID_COLUMNS) * FlaskContainer.GRID_STEP,
                    top + FlaskContainer.GRID_Y - 1
                            + (index / FlaskContainer.GRID_COLUMNS) * FlaskContainer.GRID_STEP,
                    FRAME_U, FRAME_V, 18, 18);
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
        int pipTop = pipTop();
        int rows = Math.max(1, (INVENTORY_TOP - pipTop - 2) / PIP_ROW_STEP);
        if (capacity >= 1 && capacity <= rows * PIPS_PER_ROW) {
            drawPips(used, capacity, over, pipTop);
        } else {
            // More potency than the space below the grid can show as pips, or none at all:
            // the numbers say the same thing and always fit.
            fontRenderer.drawString(used + " / " + capacity, FlaskContainer.GRID_X, pipTop,
                    over ? TEXT_OVER : 0x404040);
        }
        if (over) {
            String warning = I18n.format("everfillingflasks.screen.overCapacity");
            fontRenderer.drawString(warning,
                    xSize - 8 - fontRenderer.getStringWidth(warning), 6, TEXT_OVER);
        }
    }

    /**
     * Where the potency display starts: under whatever the grid actually occupies. A Flask with
     * a second row of slots pushes it down, which is why it is worked out rather than fixed.
     */
    private int pipTop() {
        int slots = ((FlaskContainer) inventorySlots).activeInfusionSlots();
        int gridRows = Math.max(1,
                (slots + FlaskContainer.GRID_COLUMNS - 1) / FlaskContainer.GRID_COLUMNS);
        return FlaskContainer.GRID_Y + gridRows * FlaskContainer.GRID_STEP + PIP_GAP;
    }

    /**
     * One pip per potency point, in rows of ten; filled per used point; every pip red when
     * over capacity.
     */
    private void drawPips(int used, int capacity, boolean over, int pipTop) {
        for (int i = 0; i < capacity; i++) {
            int x = FlaskContainer.GRID_X + (i % PIPS_PER_ROW) * PIP_STEP;
            int y = pipTop + (i / PIPS_PER_ROW) * PIP_ROW_STEP;
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
                && mouseX >= guiLeft + JOURNAL_X && mouseX < guiLeft + JOURNAL_X + JOURNAL_W
                && mouseY >= guiTop + JOURNAL_Y && mouseY < guiTop + JOURNAL_Y + JOURNAL_H) {
            drawHoveringText(
                    java.util.Collections.singletonList(
                            I18n.format("everfillingflasks.journal.button")),
                    mouseX, mouseY);
        }
    }
}
