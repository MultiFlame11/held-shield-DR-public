package io.github.miche.heldshielddr;

import java.text.DecimalFormat;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ShieldTooltipHandler {
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!ShieldDrConfig.enabled || !ShieldDrConfig.showShieldTooltips) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!ShieldItemHelper.isShieldLike(stack)) {
            return;
        }

        Double itemOverride = ShieldItemHelper.getItemDamageReductionOverride(stack);
        if (itemOverride != null) {
            event.getToolTip().add(
                formatLine(
                    "Passive Shield DR",
                    itemOverride,
                    ShieldDrConfig.showItemOverrideText ? "Item Override" : null
                )
            );
            return;
        }

        Double displayedPercent = null;
        if (ShieldDrConfig.applyToPlayers) {
            displayedPercent = ShieldDrConfig.damageReductionPercent;
        } else if (!ShieldDrConfig.getEntityWhitelist().isEmpty()) {
            displayedPercent = ShieldDrConfig.damageReductionPercent;
        } else if (ShieldDrConfig.applyToAllEntities) {
            displayedPercent = ShieldDrConfig.allEntitiesDamageReductionPercent;
        }

        event.getToolTip().add(
            displayedPercent != null
                ? formatLine("Passive Shield DR", displayedPercent, null)
                : TextFormatting.GOLD + "Passive Shield DR: " + TextFormatting.RED + "No valid holders"
        );
    }

    private static String formatPercent(double percent) {
        return PERCENT_FORMAT.format(percent) + "%";
    }

    private static String formatLine(String label, double percent, String suffix) {
        String line = TextFormatting.GOLD + label + ": " + TextFormatting.GREEN + formatPercent(percent);
        if (suffix != null && !suffix.isEmpty()) {
            line += TextFormatting.DARK_GRAY + " (" + suffix + ")";
        }
        return line;
    }
}
