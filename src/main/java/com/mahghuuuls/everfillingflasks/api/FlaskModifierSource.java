package com.mahghuuuls.everfillingflasks.api;

import net.minecraft.entity.player.EntityPlayer;

/**
 * One contributor of player Flask modifiers. Register through {@link FlaskApi}; the core asks
 * every registered source when it computes a player's effective Flask values.
 *
 * <p>Called on the logical server, at drink start, at charge restore, and when state is sent
 * to the client. Keep it cheap: read equipment or state, add bonuses, return. A source that
 * throws is logged once and skipped for that computation; the other sources still apply.
 */
public interface FlaskModifierSource {

    /** Add this source's bonuses for {@code player} to {@code bonuses}. */
    void contribute(EntityPlayer player, FlaskBonuses bonuses);
}
