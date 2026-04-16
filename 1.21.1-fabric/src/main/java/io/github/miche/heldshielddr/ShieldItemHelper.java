package io.github.miche.heldshielddr;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public final class ShieldItemHelper {
    private static final Set<String> WARNED_ITEM_CLASSES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ShieldItemHelper() {
    }

    public static Double getHeldShieldDamageReductionPercent(LivingEntity living, Double defaultReductionPercent, boolean useItemOverrides, boolean useHigherValueForItemOverride) {
        if (defaultReductionPercent == null || living == null) return null;

        Double mainhand = getShieldDamageReductionPercent(living.getMainHandItem(), living, defaultReductionPercent, useItemOverrides, useHigherValueForItemOverride);
        if (mainhand != null) return mainhand;

        return getShieldDamageReductionPercent(living.getOffhandItem(), living, defaultReductionPercent, useItemOverrides, useHigherValueForItemOverride);
    }

    public static Double getItemDamageReductionOverride(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        try {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key == null) return null;
            Map<String, Double> overrides = HeldShieldDrMod.config.getItemDamageReductionOverrides();
            return overrides.get(key.toString());
        } catch (RuntimeException e) {
            warnItemFailure(item, "Threw while reading registry name for item override lookup.", e);
            return null;
        }
    }

    public static boolean isShieldLike(ItemStack stack) {
        return isShieldLike(stack, null);
    }

    public static boolean isShieldLike(ItemStack stack, LivingEntity living) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        try {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key != null && key.getPath().contains("shield")) return true;

            if (living != null) {
                return stack.getUseAnimation() == UseAnim.BLOCK && stack.getUseDuration(living) > 0;
            }
            return stack.getUseAnimation() == UseAnim.BLOCK;
        } catch (RuntimeException e) {
            warnItemFailure(item, "Threw during shield detection; treating as non-shield.", e);
            return false;
        }
    }

    private static Double getShieldDamageReductionPercent(ItemStack stack, LivingEntity living, double defaultReductionPercent, boolean useItemOverrides, boolean useHigherValueForItemOverride) {
        if (!isShieldLike(stack, living)) return null;
        if (!useItemOverrides) return defaultReductionPercent;

        Double itemOverride = getItemDamageReductionOverride(stack);
        if (itemOverride == null) return defaultReductionPercent;

        return useHigherValueForItemOverride ? Math.max(defaultReductionPercent, itemOverride) : itemOverride;
    }

    private static void warnItemFailure(Item item, String message, RuntimeException e) {
        String cls = item == null ? "unknown" : item.getClass().getName();
        if (WARNED_ITEM_CLASSES.add(cls)) {
            HeldShieldDrMod.LOGGER.warn("{} Item class: {}", message, cls, e);
        }
    }
}
