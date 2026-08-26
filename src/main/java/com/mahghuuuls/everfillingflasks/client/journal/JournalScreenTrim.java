package com.mahghuuuls.everfillingflasks.client.journal;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.button.GuiButtonBookConfig;
import vazkii.patchouli.client.book.gui.button.GuiButtonBookEdit;
import vazkii.patchouli.client.book.gui.button.GuiButtonBookHistory;
import vazkii.patchouli.client.book.gui.button.GuiButtonIndex;

/**
 * Takes the book engine's own extra controls off this mod's journal.
 *
 * <p>A book normally offers a reading history, a settings button, an editor, and an index of
 * every entry ever written. Those belong to a book someone reads cover to cover; this journal is
 * a two-section reference with a handful of entries, and the owner asked for it plain
 * (2026-08-26).
 *
 * <p>Done through Forge's own screen event rather than by touching the book engine: the controls
 * are removed from the screen after it builds itself, so nothing about the engine changes and
 * every other book in the game keeps all of them. A control the engine renames or drops simply
 * stops being found here, which costs nothing.
 */
@SideOnly(Side.CLIENT)
public final class JournalScreenTrim {

    private static boolean trimFailed;

    @SubscribeEvent
    public void onScreenBuilt(GuiScreenEvent.InitGuiEvent.Post event) {
        try {
            if (!(event.getGui() instanceof GuiBook)) {
                return;
            }
            GuiBook screen = (GuiBook) event.getGui();
            if (screen.book == null
                    || !JournalBridge.BOOK.equals(screen.book.resourceLoc)) {
                return;
            }
            java.util.Iterator<GuiButton> buttons = event.getButtonList().iterator();
            while (buttons.hasNext()) {
                if (isExtra(buttons.next())) {
                    buttons.remove();
                }
            }
        } catch (Throwable failure) {
            if (!trimFailed) {
                trimFailed = true;
                EverfillingFlasksMod.LOGGER.warn(
                        "The journal's extra book controls could not be removed; they are "
                                + "harmless and the journal is otherwise unaffected.", failure);
            }
        }
    }

    private static boolean isExtra(GuiButton button) {
        return button instanceof GuiButtonBookHistory
                || button instanceof GuiButtonBookConfig
                || button instanceof GuiButtonBookEdit
                || button instanceof GuiButtonIndex;
    }
}
