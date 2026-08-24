package com.mahghuuuls.everfillingflasks.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * The Use Flask key binding. Registered from the client proxy; drinking input handling arrives
 * in a later slice. This class exists now so the item tooltip can name the actual bound key.
 */
@SideOnly(Side.CLIENT)
public final class FlaskKeys {

    public static final KeyBinding USE_FLASK = new KeyBinding(
            "everfillingflasks.key.use_flask", Keyboard.KEY_R, "everfillingflasks.key.category");

    private FlaskKeys() {
    }

    public static void register() {
        ClientRegistry.registerKeyBinding(USE_FLASK);
    }

    /** The key's current display name, for example "R", following rebinds. */
    public static String useFlaskName() {
        return USE_FLASK.getDisplayName();
    }
}
