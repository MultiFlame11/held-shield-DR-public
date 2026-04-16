package io.github.miche.heldshielddr;

import java.text.DecimalFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ShieldTooltipHandler {
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!ShieldDrConfig.ENABLED.get() || !ShieldDrConfig.SHOW_SHIELD_TOOLTIPS.get()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!ShieldItemHelper.isShieldLike(stack)) {
            return;
        }

        Double itemOverride = ShieldItemHelper.getItemDamageReductionOverride(stack);
        if (itemOverride != null) {
            event.getToolTip().add(
                formatLine("Passive Shield DR", itemOverride, ShieldDrConfig.SHOW_ITEM_OVERRIDE_TEXT.get() ? "Item Override" : null)
            );
            return;
        }

        Double displayedPercent = null;
        if (ShieldDrConfig.APPLY_TO_PLAYERS.get()) {
            displayedPercent = ShieldDrConfig.DAMAGE_REDUCTION_PERCENT.get();
        } else if (!ShieldDrConfig.getEntityWhitelist().isEmpty()) {
            displayedPercent = ShieldDrConfig.DAMAGE_REDUCTION_PERCENT.get();
        } else if (ShieldDrConfig.APPLY_TO_ALL_ENTITIES.get()) {
            displayedPercent = ShieldDrConfig.ALL_ENTITIES_DAMAGE_REDUCTION_PERCENT.get();
        }

        event.getToolTip().add(
            displayedPercent != null
                ? formatLine("Passive Shield DR", displayedPercent, null)
                : Component.literal("Passive Shield DR: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("No valid holders").withStyle(ChatFormatting.RED))
        );
    }

    private static String formatPercent(double percent) {
        return PERCENT_FORMAT.format(percent) + "%";
    }

    private static Component formatLine(String label, double percent, String suffix) {
        Component line = Component.literal(label + ": ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(formatPercent(percent)).withStyle(ChatFormatting.GREEN));
        if (suffix != null && !suffix.isEmpty()) {
            line = line.copy().append(Component.literal(" (" + suffix + ")").withStyle(ChatFormatting.DARK_GRAY));
        }
        return line;
    }
}
