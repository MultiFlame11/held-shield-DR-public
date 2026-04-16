package io.github.miche.heldshielddr;

import java.util.Map;
import java.util.Set;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class ShieldDamageHandler {
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        LivingEntity living = event.getEntityLiving();
        if (living.level.isClientSide || event.getAmount() <= 0.0F) {
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

        boolean isPlayer = living instanceof PlayerEntity;
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

        float originalAmount = event.getAmount();
        float reducedAmount = originalAmount * ShieldDrConfig.getDamageMultiplier(heldShieldReductionPercent);
        event.setAmount(reducedAmount);

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

        Hand shieldHand = null;
        ItemStack shieldStack = null;

        if (ShieldItemHelper.isShieldLike(living.getMainHandItem(), living)) {
            shieldHand = Hand.MAIN_HAND;
            shieldStack = living.getMainHandItem();
        } else if (ShieldItemHelper.isShieldLike(living.getOffhandItem(), living)) {
            shieldHand = Hand.OFF_HAND;
            shieldStack = living.getOffhandItem();
        }

        if (shieldStack == null || shieldStack.isEmpty()) return;

        final Hand hand = shieldHand;
        shieldStack.hurtAndBreak(drainInt, living, e -> e.broadcastBreakEvent(hand));
    }

    private static DamageReductionContext getDamageReductionContext(LivingEntity living) {
        if (living instanceof PlayerEntity) {
            return ShieldDrConfig.APPLY_TO_PLAYERS.get() ? new DamageReductionContext(ShieldDrConfig.DAMAGE_REDUCTION_PERCENT.get(), true) : null;
        }

        ResourceLocation entityKey = ForgeRegistries.ENTITIES.getKey(living.getType());
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
        private static final Set<String> WARNED_ENTITY_CLASSES = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

        private ShieldDamageHandlerWarnings() {
        }
    }
}
