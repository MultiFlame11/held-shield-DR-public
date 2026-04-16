package io.github.miche.heldshielddr;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public class ShieldDamageHandler {
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide || event.getNewDamage() <= 0.0F) {
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

        boolean isPlayer = living instanceof Player;
        Double heldShieldReductionPercent;
        try {
            heldShieldReductionPercent = ShieldItemHelper.getHeldShieldDamageReductionPercent(
                living,
                context.reductionPercent,
                isPlayer ? ShieldDrConfig.PLAYERS_USE_ITEM_OVERRIDES.get() : ShieldDrConfig.ENTITIES_USE_ITEM_OVERRIDES.get() && context.allowEntityItemOverrides,
                !isPlayer && context.allowEntityItemOverrides
            );
        } catch (RuntimeException e) {
            warnEntityFailure(living, "Failed to inspect held items for shield detection; skipping protection for this hit.", e);
            return;
        }

        if (heldShieldReductionPercent == null) {
            return;
        }

        float originalAmount = event.getNewDamage();
        float reducedAmount = originalAmount * ShieldDrConfig.getDamageMultiplier(heldShieldReductionPercent);
        event.setNewDamage(reducedAmount);

        if (ShieldDrConfig.SHIELD_DURABILITY_DRAIN_ENABLED.get()) {
            applyDurabilityDrain(living, originalAmount - reducedAmount);
        }
    }

    private static void applyDurabilityDrain(LivingEntity living, float damagePrevented) {
        if (damagePrevented <= 0.0F) return;

        double multiplier = ShieldDrConfig.SHIELD_DURABILITY_DRAIN_MULTIPLIER.get();
        double cap = ShieldDrConfig.SHIELD_DURABILITY_DRAIN_CAP.get();
        double rawDrain = damagePrevented * multiplier;
        double drain = (cap > 0.0) ? Math.min(rawDrain, cap) : rawDrain;
        int drainInt = Math.max(1, (int) Math.round(drain));

        if (ShieldItemHelper.isShieldLike(living.getMainHandItem(), living)) {
            living.getMainHandItem().hurtAndBreak(drainInt, living, EquipmentSlot.MAINHAND);
        } else if (ShieldItemHelper.isShieldLike(living.getOffhandItem(), living)) {
            living.getOffhandItem().hurtAndBreak(drainInt, living, EquipmentSlot.OFFHAND);
        }
    }

    private static DamageReductionContext getDamageReductionContext(LivingEntity living) {
        if (living instanceof Player) {
            return ShieldDrConfig.APPLY_TO_PLAYERS.get() ? new DamageReductionContext(ShieldDrConfig.DAMAGE_REDUCTION_PERCENT.get(), true) : null;
        }

        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
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
            return new DamageReductionContext(ShieldDrConfig.DAMAGE_REDUCTION_PERCENT.get(), true);
        }

        return ShieldDrConfig.APPLY_TO_ALL_ENTITIES.get() ? new DamageReductionContext(ShieldDrConfig.ALL_ENTITIES_DAMAGE_REDUCTION_PERCENT.get(), true) : null;
    }

    private static void warnEntityFailure(LivingEntity living, String message, RuntimeException e) {
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
        private static final Set<String> WARNED_ENTITY_CLASSES = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

        private ShieldDamageHandlerWarnings() {
        }
    }
}
