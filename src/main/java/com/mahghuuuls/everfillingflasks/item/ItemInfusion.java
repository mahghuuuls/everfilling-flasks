package com.mahghuuuls.everfillingflasks.item;

import com.mahghuuuls.everfillingflasks.config.ConfigSnapshot;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * One built-in Flask Infusion. As thin as the Flasks: behavior lives in the registered
 * definition, numbers live in the config, and the item exists for registry identity and a
 * tooltip that states cost and effect in the owner's wording ("hit threshold").
 */
public final class ItemInfusion extends Item {

    private final InfusionKind kind;

    public ItemInfusion(InfusionKind kind) {
        this.kind = kind;
        setRegistryName(kind.key());
        setTranslationKey("everfillingflasks." + kind.key());
    }

    public InfusionKind kind() {
        return kind;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        ConfigSnapshot.InfusionConfig config = ConfigSnapshot.current().infusion(kind);
        // The owner's presentation: a green header naming what this is, then the plain cost.
        tooltip.add(net.minecraft.util.text.TextFormatting.GREEN
                + I18n.format("everfillingflasks.tooltip.infusion.header"));
        // Orange, matching the potency pips under the infusion row.
        tooltip.add(net.minecraft.util.text.TextFormatting.GOLD
                + I18n.format("everfillingflasks.tooltip.infusion.cost", config.cost()));
        // The effect sentence comes from the definition, which is also what the journal reads,
        // so the tooltip and the journal cannot drift apart. Taken unformatted and coloured in
        // one piece: a translated sentence styles its inserted numbers separately, which left
        // the line changing colour halfway through.
        net.minecraft.util.text.ITextComponent effect =
                new BuiltinInfusionDefinition(kind).effectDescription(stack);
        if (effect != null) {
            tooltip.add(net.minecraft.util.text.TextFormatting.WHITE + effect.getUnformattedText());
        }
    }
}
