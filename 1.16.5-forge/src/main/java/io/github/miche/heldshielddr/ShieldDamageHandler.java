package io.github.miche.heldshielddr;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class ShieldDamageHandler {
    private static final Map<UUID, Long> LAST_PROC_SOUND_TICKS = new ConcurrentHashMap<>();
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

        if (heldShieldReductionPercent == null || !rollPassiveBlock(living)) {
            return;
        }

        double finalReductionPercent = resolveDamageReductionPercent(living, heldShieldReductionPercent);
        float originalAmount = event.getAmount();
        float reducedAmount = originalAmount * ShieldDrConfig.getDamageMultiplier(finalReductionPercent);
        event.setAmount(reducedAmount);
        if (reducedAmount < originalAmount) {
            playProcSound(living);
        }

        if (ShieldDrConfig.SHIELD_DURABILITY_DRAIN_ENABLED.get()) {
            applyDurabilityDrain(living, originalAmount - reducedAmount);
        }
    }
    private static boolean rollPassiveBlock(LivingEntity living) {
        double chance = Math.max(0.0D, Math.min(1.0D, ShieldDrConfig.PASSIVE_BLOCK_CHANCE.get()));
        return chance >= 1.0D || (chance > 0.0D && living.getRandom().nextDouble() < chance);
    }

    private static double resolveDamageReductionPercent(LivingEntity living, double basePercent) {
        String mode = ShieldDrConfig.DAMAGE_REDUCTION_MODE.get();
        if (mode == null || !mode.trim().equalsIgnoreCase("random_range")) {
            return basePercent;
        }

        double min = Math.max(0.0D, Math.min(100.0D, ShieldDrConfig.DAMAGE_REDUCTION_MIN_PERCENT.get()));
        double max = Math.max(0.0D, Math.min(100.0D, ShieldDrConfig.DAMAGE_REDUCTION_MAX_PERCENT.get()));
        if (max < min) {
            double swap = min;
            min = max;
            max = swap;
        }
        return min + living.getRandom().nextDouble() * (max - min);
    }

    private static void playProcSound(LivingEntity living) {
        String rawId = ShieldDrConfig.PROC_SOUND_ID.get();
        if (rawId == null || rawId.trim().isEmpty()) {
            return;
        }

        long now = living.level.getGameTime();
        int cooldown = Math.max(0, ShieldDrConfig.PROC_SOUND_COOLDOWN_TICKS.get());
        UUID id = living.getUUID();
        Long last = LAST_PROC_SOUND_TICKS.get(id);
        if (last != null && now - last < cooldown) {
            return;
        }

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(rawId.trim()));
        if (sound == null) {
            return;
        }

        LAST_PROC_SOUND_TICKS.put(id, now);
        living.level.playSound(null, living.getX(), living.getY(), living.getZ(), sound, SoundCategory.PLAYERS, ShieldDrConfig.PROC_SOUND_VOLUME.get().floatValue(), ShieldDrConfig.PROC_SOUND_PITCH.get().floatValue());
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
