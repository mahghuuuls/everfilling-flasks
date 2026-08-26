package com.mahghuuuls.everfillingflasks.client.journal;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.Tags;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * The only place outside {@link JournalBuilder} that names a Patchouli type.
 *
 * <p>Patchouli is a required dependency, so its absence is a startup error rather than something
 * this class handles. What it does handle is a fault: a Patchouli version whose behaviour differs
 * from the one this mod was built against must cost the journal and nothing else, so every call
 * here is guarded and reports once per session (REQ-043). The same discipline the HUD replacement
 * and modifier sources already use.
 */
@SideOnly(Side.CLIENT)
public final class JournalBridge {

    /** The book this mod ships, at {@code assets/<modid>/patchouli_books/journal/}. */
    public static final ResourceLocation BOOK = new ResourceLocation(Tags.MOD_ID, "journal");

    private static boolean openFailed;
    private static boolean iconFailed;

    /** Resolved once, then reused: the button redraws every frame. */
    private static ItemStack buttonIcon;

    private JournalBridge() {}

    /** Opens the journal for the player at their own screen. Never throws. */
    public static void open() {
        try {
            JournalBuilder.ensureCurrent();
            PatchouliAPI.instance.openBookGUI(BOOK);
        } catch (Throwable failure) {
            if (!openFailed) {
                openFailed = true;
                EverfillingFlasksMod.LOGGER.warn(
                        "The journal could not be opened; the rest of the mod is unaffected.",
                        failure);
            }
        }
    }

    /**
     * The stack drawn on the journal button: the book itself, so the button and what it opens
     * look like one thing. A plain book stands in if Patchouli hands back nothing usable.
     */
    public static ItemStack buttonIcon() {
        // Asked for once and kept: the button draws every frame the screen is open, and the
        // answer cannot change while the game runs.
        if (buttonIcon != null) {
            return buttonIcon;
        }
        try {
            ItemStack stack = PatchouliAPI.instance.getBookStack(BOOK.toString());
            if (stack != null && !stack.isEmpty()) {
                buttonIcon = stack;
                return buttonIcon;
            }
        } catch (Throwable failure) {
            if (!iconFailed) {
                iconFailed = true;
                EverfillingFlasksMod.LOGGER.warn(
                        "The journal button icon fell back to a plain book.", failure);
            }
        }
        buttonIcon = new ItemStack(Items.BOOK);
        return buttonIcon;
    }
}
