package com.mahghuuuls.everfillingflasks.flask;

import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Turns what an infusion contributes into the lines its tooltip shows (REQ-053), with no
 * Minecraft imports: the words come from the language file, this only decides which channels
 * to mention, in what order, and with what signed number.
 *
 * <p>The order is fixed: healing, drink speed, hit threshold, recharge speed, max charges,
 * effect power. Percentages are whole numbers rounded half away from zero, so +0.155 reads
 * "+16%" and -0.155 reads "-16%"; a channel that rounds to zero is left out. The sign is
 * always written, because a trade-off infusion's negative line must read as one at a glance.
 */
public final class BonusDescriptions {

    /** One tooltip line: the language-key suffix for the channel, and the signed value text. */
    public static final class Line {

        private final String channel;
        private final String value;

        Line(String channel, String value) {
            this.channel = channel;
            this.value = value;
        }

        /** The channel's key suffix, for {@code everfillingflasks.tooltip.bonus.<channel>}. */
        public String channel() {
            return channel;
        }

        /** The signed value, ready to insert: "+20%", "-15%", "+1". */
        public String value() {
            return value;
        }
    }

    private BonusDescriptions() {
    }

    public static List<Line> describe(FlaskBonuses bonuses) {
        List<Line> lines = new ArrayList<Line>();
        percent(lines, "healing", bonuses.healingSum());
        percent(lines, "drinkSpeed", bonuses.drinkSpeedSum());
        percent(lines, "hitThreshold", bonuses.hitResistanceSum());
        percent(lines, "rechargeSpeed", bonuses.rechargeSpeedSum());
        int charges = bonuses.maxChargesFlat();
        if (charges != 0) {
            lines.add(new Line(Math.abs(charges) == 1 ? "maxCharge" : "maxCharges",
                    signed(charges) + ""));
        }
        percent(lines, "effectPower", bonuses.effectPowerSum());
        return Collections.unmodifiableList(lines);
    }

    private static void percent(List<Line> lines, String channel, float fraction) {
        long whole = Math.round(Math.abs((double) fraction) * 100.0);
        if (whole == 0) {
            return;
        }
        lines.add(new Line(channel, (fraction < 0 ? "-" : "+") + whole + "%"));
    }

    private static String signed(int value) {
        return (value < 0 ? "-" : "+") + Math.abs(value);
    }
}
