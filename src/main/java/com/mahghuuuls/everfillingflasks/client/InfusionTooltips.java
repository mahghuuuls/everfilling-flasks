package com.mahghuuuls.everfillingflasks.client;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import com.mahghuuuls.everfillingflasks.api.FlaskBonuses;
import com.mahghuuuls.everfillingflasks.api.InfusionDefinition;
import com.mahghuuuls.everfillingflasks.flask.BonusDescriptions;
import com.mahghuuuls.everfillingflasks.flask.InfusionRegistry;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The one tooltip writer for every registered infusion, built-in or from an add-on (REQ-053).
 *
 * <p>Inserted directly under the item name: the green header, the orange potency cost, then
 * what it does, in white. What it does is the definition's own effect sentence when it has
 * one; otherwise one line per bonus channel its contribution changes, signed. Because this
 * runs after the item's own {@code addInformation}, any lines an add-on writes itself end up
 * after these, which is the documented order: the core writes the standard lines, the add-on
 * writes only its extras.
 *
 * <p>The definition is asked with no player, as the journal already does. A definition that
 * throws while being described keeps its header and cost, loses the generated lines, and is
 * named in the log once.
 */
@SideOnly(Side.CLIENT)
public final class InfusionTooltips {

    private final Set<String> failed = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        InfusionDefinition definition = InfusionRegistry.definition(stack);
        if (definition == null) {
            return;
        }
        List<String> lines = new ArrayList<String>();
        lines.add(TextFormatting.GREEN + I18n.format("everfillingflasks.tooltip.infusion.header"));
        lines.add(TextFormatting.GOLD
                + I18n.format("everfillingflasks.tooltip.infusion.cost", cost(definition, stack)));
        lines.addAll(effectLines(definition, stack));

        List<String> tooltip = event.getToolTip();
        // Under the name, which vanilla always puts first.
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    private int cost(InfusionDefinition definition, ItemStack stack) {
        try {
            return Math.max(0, definition.potencyCost(stack));
        } catch (Throwable failure) {
            logOnce(definition, "potencyCost", failure);
            return 0;
        }
    }

    private List<String> effectLines(InfusionDefinition definition, ItemStack stack) {
        List<String> lines = new ArrayList<String>();
        try {
            // Taken unformatted and coloured in one piece: a translated sentence styles its
            // inserted numbers separately, which left the line changing colour halfway through.
            ITextComponent sentence = definition.effectDescription(stack);
            if (sentence != null) {
                lines.add(TextFormatting.WHITE + sentence.getUnformattedText());
                return lines;
            }
            FlaskBonuses bonuses = new FlaskBonuses();
            definition.contribute(stack, null, bonuses);
            for (BonusDescriptions.Line line : BonusDescriptions.describe(bonuses)) {
                lines.add(TextFormatting.WHITE + I18n.format(
                        "everfillingflasks.tooltip.bonus." + line.channel(), line.value()));
            }
        } catch (Throwable failure) {
            logOnce(definition, "tooltip description", failure);
            lines.clear();
        }
        return lines;
    }

    private void logOnce(InfusionDefinition definition, String what, Throwable failure) {
        if (failed.add(definition.getClass().getName() + "#" + what)) {
            EverfillingFlasksMod.LOGGER.error(
                    "Infusion definition {} failed in {} while its tooltip was written; its"
                            + " generated lines are left out, and this is logged once",
                    definition.getClass().getName(), what, failure);
        }
    }
}
