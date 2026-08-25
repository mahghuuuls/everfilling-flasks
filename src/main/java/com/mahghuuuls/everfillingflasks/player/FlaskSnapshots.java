package com.mahghuuuls.everfillingflasks.player;

import com.mahghuuuls.everfillingflasks.api.FlaskSnapshot;
import com.mahghuuuls.everfillingflasks.flask.FlaskRegistry;
import com.mahghuuuls.everfillingflasks.flask.FlaskStackState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * The server-side snapshot builder behind {@code FlaskApi.snapshot}. Reads the same sources
 * the state message is built from — the capability, the stored stack state, and the effective
 * values — so an API caller and the owning client see the same numbers for the same moment.
 * The one difference is the stack: the API receives a copy, because a snapshot must never be
 * a write path into the live slot.
 */
public final class FlaskSnapshots {

    private FlaskSnapshots() {
    }

    /** The no-Flask snapshot, also the safe answer wherever no state exists. */
    public static FlaskSnapshot empty() {
        return new FlaskSnapshot(ItemStack.EMPTY, 0, 0, 0, 1, false, false, 0, 1, 0.0F);
    }

    /** This player's state as the server knows it right now. */
    public static FlaskSnapshot server(EntityPlayerMP player) {
        FlaskPlayerData data = FlaskPlayerCapability.get(player);
        if (data == null) {
            return empty();
        }
        // The tracked stack, not the slot's current content: like the state message, every
        // field of one snapshot must describe the same stack, and in the sub-tick window
        // after a container click the two can briefly differ.
        ItemStack flask = data.trackedStack;
        if (flask.isEmpty() || !FlaskRegistry.isFlask(flask)) {
            return empty();
        }
        com.mahghuuuls.everfillingflasks.flask.EffectiveFlask effective =
                DrinkController.effectiveForApi(player, data, flask);
        int charges = com.mahghuuuls.everfillingflasks.flask.FlaskMechanics.clampCharges(
                FlaskStackState.charges(flask), effective.maxCharges());
        int progress = data.liveValid ? data.liveProgress : FlaskStackState.progress(flask);
        return new FlaskSnapshot(flask.copy(), charges, effective.maxCharges(), progress,
                effective.rechargeTicks(), data.rechargePaused, data.drinking,
                data.drinkElapsed,
                data.drinking ? data.drinkEffective.drinkTicks() : effective.drinkTicks(),
                effective.hitThreshold());
    }
}
