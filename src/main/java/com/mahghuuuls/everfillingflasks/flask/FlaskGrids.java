package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.api.FlaskDefinition;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * How many infusion slots a particular Flask has.
 *
 * <p>One place answers this for the whole mod, because storage, the container, the screen, and
 * the tooltip must never disagree about it. The Flask's own definition decides; the answer is
 * brought inside the range the screen can draw, and a definition that asks for something outside
 * it is named once so its author can find out.
 *
 * <p>A stack that is not a Flask, or a definition that throws, answers with the default rather
 * than with nothing: a grid of the usual size is always safer than no grid at all.
 */
public final class FlaskGrids {

    /** One log line per offending definition class per session. */
    private static final Set<String> reported =
            Collections.synchronizedSet(new HashSet<String>());

    private FlaskGrids() {
    }

    public static int slots(ItemStack flask) {
        FlaskDefinition definition = FlaskRegistry.definition(flask);
        if (definition == null) {
            return FlaskStackState.DEFAULT_GRID_SIZE;
        }
        int declared;
        try {
            declared = definition.infusionSlots(flask);
        } catch (Exception failure) {
            report(definition, "threw when asked for its infusion slot count", failure);
            return FlaskStackState.DEFAULT_GRID_SIZE;
        }
        int slots = FlaskMechanics.infusionSlots(declared);
        if (slots != declared) {
            report(definition, "asked for " + declared + " infusion slots; "
                    + slots + " is the nearest the screen can draw", null);
        }
        return slots;
    }

    private static void report(FlaskDefinition definition, String what, Throwable failure) {
        String name = definition.getClass().getName();
        if (!reported.add(name)) {
            return;
        }
        if (failure == null) {
            EverfillingFlasksMod.LOGGER.warn("Flask definition {} {}.", name, what);
        } else {
            EverfillingFlasksMod.LOGGER.warn("Flask definition {} {}.", name, what, failure);
        }
    }
}
