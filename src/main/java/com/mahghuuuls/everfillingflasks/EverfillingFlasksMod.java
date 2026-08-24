package com.mahghuuuls.everfillingflasks;

import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.network.FlaskGuiHandler;
import com.mahghuuuls.everfillingflasks.network.PacketHandler;
import com.mahghuuuls.everfillingflasks.player.FlaskPlayerCapability;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for Everfilling Flasks.
 *
 * <p>Loads on both sides. The server owns every Flask decision; the client draws, listens for the
 * key, and opens the Flask screen through Inventory Button Bar, which is why that mod is required
 * on both sides even though it does nothing on a server.
 *
 * <p>Dependency ranges are minimum-only on purpose: the versions named are the ones this mod was
 * built and tested against, and nothing newer is excluded.
 */
@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        dependencies = "required-after:forge@[14.23.5.2847,);required-after:inventorybuttonbar@[1.0.0,)")
public class EverfillingFlasksMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.Instance(Tags.MOD_ID)
    private static EverfillingFlasksMod instance;

    @SidedProxy(
            clientSide = "com.mahghuuuls.everfillingflasks.client.ClientProxy",
            serverSide = "com.mahghuuuls.everfillingflasks.CommonProxy")
    public static CommonProxy proxy;

    public static EverfillingFlasksMod instance() {
        return instance;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ConfigSnapshot.refresh();
        for (String warning : ConfigSnapshot.current().clampWarnings()) {
            LOGGER.warn(warning);
        }
        FlaskPlayerCapability.register();
        PacketHandler.register();
        com.mahghuuuls.everfillingflasks.flask.InternalFlaskApiBridge.install();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new FlaskGuiHandler());
        proxy.registerSidedHandlers();
    }
}
