package io.github.miche.heldshielddr;

import java.text.DecimalFormat;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ShieldTooltipHandler implements ItemTooltipCallback {
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    @Override
    public void getTooltip(ItemStack stack, TooltipFlag tooltipFlag, List<Component> lines) {
        ShieldDrConfig config = HeldShieldDrMod.config;
        if (config == null || !config.enabled || !config.showShieldTooltips) return;
        if (!ShieldItemHelper.isShieldLike(stack)) return;

        Double itemOverride = ShieldItemHelper.getItemDamageReductionOverride(stack);
        if (itemOverride != null) {
            lines.add(formatLine("Passive Shield DR", itemOverride, config.showItemOverrideText ? "Item Override" : null));
            return;
        }

        Double displayPercent = null;
        if (config.applyToPlayers) {
            displayPercent = config.damageReductionPercent;
        } else if (!config.getEntityWhitelist().isEmpty()) {
            displayPercent = config.damageReductionPercent;
        } else if (config.applyToAllEntities) {
            displayPercent = config.allEntitiesDamageReductionPercent;
        }

        lines.add(displayPercent != null
            ? formatLine("Passive Shield DR", displayPercent, null)
            : new TextComponent("Passive Shield DR: ").withStyle(ChatFormatting.GOLD)
                .append(new TextComponent("No valid holders").withStyle(ChatFormatting.RED)));
    }

    private static Component formatLine(String label, double percent, String suffix) {
        MutableComponent line = new TextComponent(label + ": ").withStyle(ChatFormatting.GOLD)
            .append(new TextComponent(PERCENT_FORMAT.format(percent) + "%").withStyle(ChatFormatting.GREEN));
        if (suffix != null && !suffix.isEmpty()) {
            line = line.copy().append(new TextComponent(" (" + suffix + ")").withStyle(ChatFormatting.DARK_GRAY));
        }
        return line;
    }
}
