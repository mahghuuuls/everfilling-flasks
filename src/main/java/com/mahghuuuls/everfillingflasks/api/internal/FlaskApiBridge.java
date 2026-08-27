package com.mahghuuuls.everfillingflasks.api.internal;

import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import com.mahghuuuls.everfillingflasks.api.FlaskModifierSource;
import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The seam between the public {@code api} package and the mod's internals. Add-ons must not
 * touch this package: its shape may change between versions without notice, and everything in
 * it is reachable through {@link com.mahghuuuls.everfillingflasks.api.FlaskApi}.
 *
 * <p>Calls arriving before the mod's pre-initialization are buffered and replayed when the
 * implementation binds, so an add-on's registration order against this mod cannot matter.
 */
public abstract class FlaskApiBridge {

    private static FlaskApiBridge instance;
    private static List<FlaskModifierSource> pendingSources = new ArrayList<FlaskModifierSource>();
    private static List<Object[]> pendingInfusions = new ArrayList<Object[]>();
    private static List<Object[]> pendingFlasks = new ArrayList<Object[]>();
    private static List<Object[]> pendingJournalItems = new ArrayList<Object[]>();

    public static synchronized void bind(FlaskApiBridge implementation) {
        if (instance != null) {
            org.apache.logging.log4j.LogManager.getLogger("Everfilling Flasks").warn(
                    "Flask API bridge rebound from {} to {}; only the mod itself should bind",
                    instance.getClass().getName(), implementation.getClass().getName());
        }
        instance = implementation;
        if (pendingSources != null) {
            for (FlaskModifierSource source : pendingSources) {
                implementation.registerModifierSourceNow(source);
            }
            pendingSources = null;
        }
        if (pendingInfusions != null) {
            for (Object[] pending : pendingInfusions) {
                implementation.registerInfusionNow((Item) pending[0],
                        (InfusionDefinition) pending[1]);
            }
            pendingInfusions = null;
        }
        if (pendingFlasks != null) {
            for (Object[] pending : pendingFlasks) {
                implementation.registerFlaskNow((Item) pending[0],
                        (FlaskDefinition) pending[1]);
            }
            pendingFlasks = null;
        }
        if (pendingJournalItems != null) {
            for (Object[] pending : pendingJournalItems) {
                implementation.registerJournalItemNow((Item) pending[0], (String) pending[1]);
            }
            pendingJournalItems = null;
        }
    }

    public static synchronized void registerModifierSource(FlaskModifierSource source) {
        if (instance == null) {
            pendingSources.add(source);
        } else {
            instance.registerModifierSourceNow(source);
        }
    }

    public static synchronized void registerInfusion(Item item,
                                                       InfusionDefinition definition) {
        if (instance == null) {
            pendingInfusions.add(new Object[]{item, definition});
        } else {
            instance.registerInfusionNow(item, definition);
        }
    }

    public static synchronized void registerJournalItem(Item item, String textKey) {
        if (instance == null) {
            pendingJournalItems.add(new Object[]{item, textKey});
        } else {
            instance.registerJournalItemNow(item, textKey);
        }
    }

    public static synchronized void registerFlask(Item item, FlaskDefinition definition) {
        if (instance == null) {
            pendingFlasks.add(new Object[]{item, definition});
        } else {
            instance.registerFlaskNow(item, definition);
        }
    }

    /** False before the mod binds: nothing can be a Flask before the framework exists. */
    public static boolean isFlask(ItemStack stack) {
        FlaskApiBridge bridge = instance;
        return bridge != null && bridge.isFlaskNow(stack);
    }

    /** Null before the mod binds, like any unregistered item. */
    public static FlaskDefinition definition(ItemStack stack) {
        FlaskApiBridge bridge = instance;
        return bridge == null ? null : bridge.definitionNow(stack);
    }

    /** The empty snapshot before the mod binds; a query can never throw for timing. */
    public static FlaskSnapshot snapshot(EntityPlayer player) {
        FlaskApiBridge bridge = instance;
        if (bridge == null) {
            return new FlaskSnapshot(ItemStack.EMPTY, 0, 0, 0, 1, false, false, 0, 1, 0.0F);
        }
        return bridge.snapshotNow(player);
    }

    protected abstract void registerModifierSourceNow(FlaskModifierSource source);

    protected abstract void registerInfusionNow(Item item, InfusionDefinition definition);

    protected abstract void registerFlaskNow(Item item, FlaskDefinition definition);

    protected abstract void registerJournalItemNow(Item item, String textKey);

    protected abstract boolean isFlaskNow(ItemStack stack);

    protected abstract FlaskDefinition definitionNow(ItemStack stack);

    protected abstract FlaskSnapshot snapshotNow(EntityPlayer player);
}
