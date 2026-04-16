package io.github.miche.heldshielddr;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public final class ShieldItemHelper {
    private static final Set<String> WARNED_ITEM_CLASSES = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private ShieldItemHelper() {
    }

    public static Double getHeldShieldDamageReductionPercent(LivingEntity living, Double defaultReductionPercent, boolean useItemOverrides, boolean useHigherValueForItemOverride) {
        if (defaultReductionPercent == null || living == null) {
            return null;
        }

        Double mainhandReduction = getShieldDamageReductionPercent(
            living.getMainHandItem(),
            living,
            defaultReductionPercent,
            useItemOverrides,
            useHigherValueForItemOverride
        );
        if (mainhandReduction != null) {
            return mainhandReduction;
        }

        return getShieldDamageReductionPercent(
            living.getOffhandItem(),
            living,
            defaultReductionPercent,
            useItemOverrides,
            useHigherValueForItemOverride
        );
    }

    public static Double getItemDamageReductionOverride(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        Item item = stack.getItem();
        ResourceLocation registryName;
        try {
            registryName = ForgeRegistries.ITEMS.getKey(item);
        } catch (RuntimeException e) {
            warnItemFailure(item, "Encountered an item that threw while reading its registry name; skipping item override lookup.", e);
            return null;
        }

        if (registryName == null) {
            return null;
        }

        Map<String, Double> overrides = ShieldDrConfig.getItemDamageReductionOverrides();
        return overrides.get(registryName.toString());
    }

    public static boolean isShieldLike(ItemStack stack) {
        return isShieldLike(stack, null);
    }

    public static boolean isShieldLike(ItemStack stack, LivingEntity living) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        try {
            if (item.isShield(stack, living)) {
                return true;
            }

            ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
            if (registryName != null && registryName.getPath().contains("shield")) {
                return true;
            }

            return stack.getUseAnimation() == net.minecraft.item.UseAction.BLOCK && stack.getUseDuration() > 0;
        } catch (RuntimeException e) {
            warnItemFailure(item, "Encountered an item that threw during held shield detection; treating it as non-shield.", e);
            return false;
        }
    }

    private static Double getShieldDamageReductionPercent(
        ItemStack stack,
        LivingEntity living,
        double defaultReductionPercent,
        boolean useItemOverrides,
        boolean useHigherValueForItemOverride
    ) {
        if (!isShieldLike(stack, living)) {
            return null;
        }

        if (!useItemOverrides) {
            return defaultReductionPercent;
        }

        Double itemOverride = getItemDamageReductionOverride(stack);
        if (itemOverride == null) {
            return defaultReductionPercent;
        }

        return useHigherValueForItemOverride ? Math.max(defaultReductionPercent, itemOverride) : itemOverride;
    }

    private static void warnItemFailure(Item item, String message, RuntimeException e) {
        String itemClass = item == null ? "unknown" : item.getClass().getName();
        if (WARNED_ITEM_CLASSES.add(itemClass)) {
            HeldShieldDrMod.LOGGER.warn("{} Item class: {}", message, itemClass, e);
        }
    }
}
