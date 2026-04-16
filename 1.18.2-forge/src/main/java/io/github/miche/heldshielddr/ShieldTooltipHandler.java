package io.github.miche.heldshielddr;

import java.text.DecimalFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
                formatLine(
                    "Passive Shield DR",
                    itemOverride,
                    ShieldDrConfig.SHOW_ITEM_OVERRIDE_TEXT.get() ? "Item Override" : null
                )
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
                : new TextComponent("Passive Shield DR: ").withStyle(ChatFormatting.GOLD)
                    .append(new TextComponent("No valid holders").withStyle(ChatFormatting.RED))
        );
    }

    private static String formatPercent(double percent) {
        return PERCENT_FORMAT.format(percent) + "%";
    }

    private static MutableComponent formatLine(String label, double percent, String suffix) {
        MutableComponent line = new TextComponent(label + ": ").withStyle(ChatFormatting.GOLD)
            .append(new TextComponent(formatPercent(percent)).withStyle(ChatFormatting.GREEN));
        if (suffix != null && !suffix.isEmpty()) {
            line = line.append(new TextComponent(" (" + suffix + ")").withStyle(ChatFormatting.DARK_GRAY));
        }
        return line;
    }
}
