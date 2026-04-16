package io.github.miche.heldshielddr;

import java.util.Map;
import java.util.Set;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ShieldDamageHandler {
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        if (living.world.isRemote || event.getAmount() <= 0.0F) {
            return;
        }

        DamageReductionContext context;
        try {
            context = getDamageReductionContext(living);
        } catch (RuntimeException e) {
            warnEntityFailure(living, "Failed to evaluate held shield damage reduction for entity; skipping protection for this hit.", e);
            return;
        }

        if (context == null) {
            return;
        }

        boolean isPlayer = living instanceof EntityPlayer;
        Double heldShieldReductionPercent;
        try {
            heldShieldReductionPercent = ShieldItemHelper.getHeldShieldDamageReductionPercent(
                living,
                context.reductionPercent,
                isPlayer ? ShieldDrConfig.playersUseItemOverrides : ShieldDrConfig.entitiesUseItemOverrides && context.allowEntityItemOverrides,
                !isPlayer && context.allowEntityItemOverrides
            );
        } catch (RuntimeException e) {
            warnEntityFailure(living, "Failed to inspect held items for shield detection; skipping protection for this hit.", e);
            return;
        }

        if (heldShieldReductionPercent == null) {
            return;
        }

        float originalAmount = event.getAmount();
        float reducedAmount = originalAmount * ShieldDrConfig.getDamageMultiplier(heldShieldReductionPercent);
        event.setAmount(reducedAmount);

        if (ShieldDrConfig.shieldDurabilityDrainEnabled) {
            applyDurabilityDrain(living, originalAmount - reducedAmount);
        }
    }

    private static void applyDurabilityDrain(EntityLivingBase living, float damagePrevented) {
        if (damagePrevented <= 0.0F) return;

        double multiplier = ShieldDrConfig.shieldDurabilityDrainMultiplier;
        double cap = ShieldDrConfig.shieldDurabilityDrainCap;
        double rawDrain = damagePrevented * multiplier;
        double drain = (cap > 0.0) ? Math.min(rawDrain, cap) : rawDrain;
        int drainInt = Math.max(1, (int) Math.round(drain));

        ItemStack shieldStack = null;

        if (ShieldItemHelper.isShieldLike(living.getHeldItemMainhand(), living)) {
            shieldStack = living.getHeldItemMainhand();
        } else if (ShieldItemHelper.isShieldLike(living.getHeldItemOffhand(), living)) {
            shieldStack = living.getHeldItemOffhand();
        }

        if (shieldStack == null || shieldStack.isEmpty()) return;

        shieldStack.damageItem(drainInt, living);
    }

    private static DamageReductionContext getDamageReductionContext(EntityLivingBase living) {
        if (living instanceof EntityPlayer) {
            return ShieldDrConfig.applyToPlayers ? new DamageReductionContext(ShieldDrConfig.damageReductionPercent, true) : null;
        }

        ResourceLocation entityKey = EntityList.getKey(living);
        if (entityKey == null) {
            return null;
        }

        String entityId = entityKey.toString();
        Map<String, Double> overrides = ShieldDrConfig.getEntityDamageReductionOverrides();
        if (overrides.containsKey(entityId)) {
            return new DamageReductionContext(overrides.get(entityId), false);
        }

        Set<String> whitelist = ShieldDrConfig.getEntityWhitelist();
        if (whitelist.contains(entityId)) {
            return new DamageReductionContext(ShieldDrConfig.damageReductionPercent, true);
        }

        return ShieldDrConfig.applyToAllEntities ? new DamageReductionContext(ShieldDrConfig.allEntitiesDamageReductionPercent, true) : null;
    }

    private static void warnEntityFailure(EntityLivingBase living, String message, RuntimeException e) {
        String entityClass = living == null ? "unknown" : living.getClass().getName();
        if (ShieldDamageHandlerWarnings.WARNED_ENTITY_CLASSES.add(entityClass)) {
            HeldShieldDrMod.LOGGER.warn("{} Entity class: {}", message, entityClass, e);
        }
    }

    private static final class DamageReductionContext {
        private final double reductionPercent;
        private final boolean allowEntityItemOverrides;

        private DamageReductionContext(double reductionPercent, boolean allowEntityItemOverrides) {
            this.reductionPercent = reductionPercent;
            this.allowEntityItemOverrides = allowEntityItemOverrides;
        }
    }

    private static final class ShieldDamageHandlerWarnings {
        private static final Set<String> WARNED_ENTITY_CLASSES = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

        private ShieldDamageHandlerWarnings() {
        }
    }
}
