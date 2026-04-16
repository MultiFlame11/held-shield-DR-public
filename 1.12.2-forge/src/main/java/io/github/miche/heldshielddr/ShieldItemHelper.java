package io.github.miche.heldshielddr;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class ShieldItemHelper {
    private static final Set<String> WARNED_ITEM_CLASSES = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private ShieldItemHelper() {
    }

    public static Double getHeldShieldDamageReductionPercent(EntityLivingBase living, Double defaultReductionPercent, boolean useItemOverrides, boolean useHigherValueForItemOverride) {
        if (defaultReductionPercent == null || living == null) {
            return null;
        }

        Double mainhandReduction = getShieldDamageReductionPercent(
            living.getHeldItemMainhand(),
            living,
            defaultReductionPercent,
            useItemOverrides,
            useHigherValueForItemOverride
        );
        if (mainhandReduction != null) {
            return mainhandReduction;
        }

        return getShieldDamageReductionPercent(
            living.getHeldItemOffhand(),
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
        if (item == null) {
            return null;
        }

        ResourceLocation registryName;
        try {
            registryName = item.getRegistryName();
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

    public static boolean isShieldLike(ItemStack stack, EntityLivingBase living) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        if (item == null) {
            return false;
        }

        try {
            if (item.isShield(stack, living)) {
                return true;
            }

            ResourceLocation registryName = item.getRegistryName();
            if (registryName != null && registryName.getResourcePath().contains("shield")) {
                return true;
            }

            return stack.getItemUseAction() == EnumAction.BLOCK && stack.getMaxItemUseDuration() > 0;
        } catch (RuntimeException e) {
            warnItemFailure(item, "Encountered an item that threw during held shield detection; treating it as non-shield.", e);
            return false;
        }
    }

    private static Double getShieldDamageReductionPercent(
        ItemStack stack,
        EntityLivingBase living,
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
