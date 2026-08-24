package com.mahghuuuls.everfillingflasks.client;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.Tags;
import com.mahghuuuls.everfillingflasks.network.OpenFlaskScreenMessage;
import com.mahghuuuls.everfillingflasks.network.PacketHandler;
import com.mahghuuuls.inventorybuttonbar.api.ButtonSpec;
import com.mahghuuuls.inventorybuttonbar.api.InventoryButtonBar;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Gives the Flask button to Inventory Button Bar, which is the only way into the Flask screen.
 *
 * <p>This is deliberately the only class in the mod that names a bar type. The bar's API is
 * client-only and its classes crash a dedicated server at class load, so every reference stays
 * here, reached from the client proxy alone.
 */
@SideOnly(Side.CLIENT)
public final class ButtonBarRegistration {

    private static final ResourceLocation ICON =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/flask_button.png");

    private ButtonBarRegistration() {
    }

    public static void register() {
        try {
            InventoryButtonBar.register(ButtonSpec.builder(Tags.MOD_ID + ":flask")
                    .icon(ICON)
                    .tooltip(new Tooltip())
                    .onClick(new OpenFlaskScreen())
                    .build());
        } catch (Throwable failure) {
            // Wide on purpose, and it does not rethrow. Anything thrown here means a bar version
            // this mod was not built against; losing the button is bad, but taking the game down
            // over a button is worse. The bar is required, so there is no fallback to hide.
            EverfillingFlasksMod.LOGGER.error(
                    "Inventory Button Bar refused the flask button, so the Flask screen cannot be"
                            + " opened. The mod's items and state remain intact.", failure);
        }
    }

    /** Asked per frame while hovered, so the text follows a language change. */
    private static final class Tooltip implements Supplier<List<String>> {

        @Override
        public List<String> get() {
            return Collections.singletonList(I18n.format("everfillingflasks.button.flask"));
        }
    }

    /** The click only asks the server; the server opens the container-backed screen. */
    private static final class OpenFlaskScreen implements Runnable {

        @Override
        public void run() {
            PacketHandler.CHANNEL.sendToServer(new OpenFlaskScreenMessage());
        }
    }
}
