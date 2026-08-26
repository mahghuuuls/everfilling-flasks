package com.mahghuuuls.everfillingflasks.client.journal;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.PageCrafting;

/**
 * The book's crafting page, showing the infusion grid alone.
 *
 * <p>The book normally draws the grid, an arrow, the result, and the result's name over the top.
 * On a journal entry every one of those repeats something: the entry is already the item, and
 * its name is already the page's heading. So this draws the grid and the shapeless marker, and
 * leaves out the arrow, the result, and the title (owner decisions 2026-08-26).
 *
 * <p>The grid itself, the infusion cycling, and the lookup into Minecraft's recipe registry
 * are all still the book's own.
 */
@SideOnly(Side.CLIENT)
public class CenteredCraftingPage extends PageCrafting {

    /** The page name this is registered under, used in generated entries. */
    static final String TYPE = "everfillingflasks:crafting";

    /** The book's recipe frame is 100 by 62; the grid alone is the left 62 of it. */
    private static final int FRAME_WIDTH = 62;
    private static final int FRAME_HEIGHT = 62;

    /** Where the shapeless marker sits on the sheet, and how big it is. */
    private static final int MARKER_U = 0;
    private static final int MARKER_V = 64;
    private static final int MARKER_SIZE = 11;

    /** One grid cell to the next, and the frame's own inner margin. */
    private static final int CELL_STEP = 19;
    private static final int CELL_INSET = 3;
    private static final int SHEET_SIZE = 128;

    @Override
    protected int getY() {
        return (GuiBook.PAGE_HEIGHT - FRAME_HEIGHT) / 2;
    }

    /** The grid is narrower than the book's own frame, so it is centred on its own width. */
    @Override
    protected int getX() {
        return (GuiBook.PAGE_WIDTH - FRAME_WIDTH) / 2 + 2;
    }

    @Override
    public int getTextHeight() {
        return 0;
    }

    @Override
    protected String getTitle(boolean second) {
        return "";
    }

    @Override
    protected void drawRecipe(IRecipe recipe, int recipeX, int recipeY, int mouseX, int mouseY,
            boolean second) {
        mc.renderEngine.bindTexture(book.craftingResource);
        GlStateManager.enableBlend();
        // The book's own frame, cropped to the grid: everything right of it is the arrow and
        // the result, which this page does not show.
        Gui.drawModalRectWithCustomSizedTexture(recipeX - 2, recipeY - 2, 0, 0,
                FRAME_WIDTH, FRAME_HEIGHT, SHEET_SIZE, SHEET_SIZE);

        boolean shaped = recipe instanceof IShapedRecipe;
        if (!shaped) {
            // Kept: it says the order does not matter, which nothing else on the page says.
            int markerX = recipeX + FRAME_WIDTH;
            int markerY = recipeY + 2;
            Gui.drawModalRectWithCustomSizedTexture(markerX, markerY, MARKER_U, MARKER_V,
                    MARKER_SIZE, MARKER_SIZE, SHEET_SIZE, SHEET_SIZE);
            if (parent.isMouseInRelativeRange(mouseX, mouseY, markerX, markerY,
                    MARKER_SIZE, MARKER_SIZE)) {
                parent.setTooltip(I18n.format("patchouli.gui.lexicon.shapeless"));
            }
        }

        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int wrap = shaped ? ((IShapedRecipe) recipe).getRecipeWidth() : 3;
        for (int i = 0; i < ingredients.size(); i++) {
            parent.renderIngredient(recipeX + (i % wrap) * CELL_STEP + CELL_INSET,
                    recipeY + (i / wrap) * CELL_STEP + CELL_INSET, mouseX, mouseY,
                    ingredients.get(i));
        }
    }
}
