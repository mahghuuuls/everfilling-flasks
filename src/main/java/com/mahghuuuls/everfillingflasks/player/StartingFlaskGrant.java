package com.mahghuuuls.everfillingflasks.player;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import com.mahghuuuls.everfillingflasks.diagnostics.Diagnostics;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * The once-per-player starting Flask. Validation happens in a fixed order so the warning names
 * the first real problem: the value is set, the item exists, the item is a Flask. An invalid
 * configuration grants nothing, warns once per session, and leaves the flag unset, so a
 * corrected file grants on the player's first login after the restart that loads it.
 *
 * <p>The grant writes to the slot handler directly, which is the approved bypass of the equip
 * rule: the container slot that empties placed Flasks is not involved, so this is the one Flask
 * in the mod that begins life full.
 */
public final class StartingFlaskGrant {

    /** Values already warned about, so a bad config logs once per session, not per login. */
    private static final Set<String> warnedValues = new ConcurrentSkipListSet<String>();

    /**
     * Why a configured value granted nothing, or {@link #GRANT} when it is usable. There is no
     * malformed-name outcome: this Minecraft version's ResourceLocation accepts any string, so
     * a malformed value simply resolves to no item.
     */
    enum Outcome {
        DISABLED, NO_SUCH_ITEM, NOT_A_FLASK, GRANT
    }

    static final class Decision {

        final Outcome outcome;
        @Nullable
        final Item item;

        private Decision(Outcome outcome, @Nullable Item item) {
            this.outcome = outcome;
            this.item = item;
        }
    }

    private StartingFlaskGrant() {
    }

    /** The validation, separated from the player so its order and outcomes are testable. */
    static Decision decide(String configured) {
        if (configured.isEmpty()) {
            return new Decision(Outcome.DISABLED, null);
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(configured));
        if (item == null) {
            return new Decision(Outcome.NO_SUCH_ITEM, null);
        }
        if (!FlaskRegistry.isFlask(new ItemStack(item))) {
            return new Decision(Outcome.NOT_A_FLASK, null);
        }
        return new Decision(Outcome.GRANT, item);
    }

    /** Called at every login; the flag makes it a no-op after the first successful grant. */
    public static void tryGrant(EntityPlayer player) {
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data == null || data.startingFlaskGranted()) {
            return;
        }
        String configured = ConfigSnapshot.current().startingFlask();
        Decision decision = decide(configured);
        switch (decision.outcome) {
            case DISABLED:
                // Disabled on purpose; not a misconfiguration, so not a warning.
                return;
            case NO_SUCH_ITEM:
                warnOnce(configured, "startingFlask \"" + configured + "\" does not name a"
                        + " registered item; no starting flask was granted");
                return;
            case NOT_A_FLASK:
                warnOnce(configured, "startingFlask \"" + configured + "\" is not a Flask (no"
                        + " Flask definition is registered for it); no starting flask was"
                        + " granted");
                return;
            case GRANT:
                break;
        }
        ItemStack flask = new ItemStack(decision.item);
        int maxCharges;
        try {
            // A definition may be third-party code, and this runs inside the login sequence; a
            // throw here must cost the player a starting Flask attempt, never the login.
            maxCharges = FlaskRegistry.definition(flask).maxCharges(flask, player);
        } catch (Throwable failure) {
            EverfillingFlasksMod.LOGGER.error(
                    "The Flask definition for startingFlask \"" + configured + "\" threw while"
                            + " asked for its maximum charges; no starting flask was granted",
                    failure);
            return;
        }
        FlaskStackState.initialiseFull(flask, maxCharges);
        ItemStack granted = flask.copy();
        if (data.equippedFlask().isEmpty()) {
            data.slot().setStackInSlot(0, flask);
        } else {
            // InventoryPlayer.add mutates the stack it stores (to count 0), and in creative it
            // reports success even when nothing fit, so the drop decision reads the count.
            player.inventory.addItemStackToInventory(flask);
            if (!flask.isEmpty()) {
                player.dropItem(flask, false);
            }
        }
        data.markStartingFlaskGranted();
        Diagnostics.startingFlaskGranted(player, granted);
    }

    private static void warnOnce(String value, String message) {
        if (warnedValues.add(value)) {
            EverfillingFlasksMod.LOGGER.warn(message);
        }
    }
}
