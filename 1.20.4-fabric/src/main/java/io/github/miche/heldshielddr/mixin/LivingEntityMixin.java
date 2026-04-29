package io.github.miche.heldshielddr.mixin;

import io.github.miche.heldshielddr.HeldShieldDrMod;
import io.github.miche.heldshielddr.ShieldDrConfig;
import io.github.miche.heldshielddr.ShieldItemHelper;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final Map<UUID, Long> heldShieldDr$lastProcSoundTicks = new ConcurrentHashMap<>();

    @ModifyArg(
        method = "hurt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            ordinal = 0
        ),
        index = 1
    )
    private float heldShieldDr$modifyDamageFirst(float amount) {
        return heldShieldDr$applyPassiveShieldReduction(amount);
    }

    @ModifyArg(
        method = "hurt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            ordinal = 1
        ),
        index = 1
    )
    private float heldShieldDr$modifyDamageSecond(float amount) {
        return heldShieldDr$applyPassiveShieldReduction(amount);
    }

    private float heldShieldDr$applyPassiveShieldReduction(float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        ShieldDrConfig config = HeldShieldDrMod.config;

        if (config == null || !config.enabled || self.level().isClientSide() || amount <= 0.0F) {
            return amount;
        }

        Double reductionPercent = getReductionPercent(self, config);
        if (reductionPercent == null) return amount;

        Double heldShieldReductionPercent;
        try {
            boolean isPlayer = self instanceof Player;
            heldShieldReductionPercent = ShieldItemHelper.getHeldShieldDamageReductionPercent(
                self,
                reductionPercent,
                isPlayer ? config.playersUseItemOverrides : config.entitiesUseItemOverrides && !isEntityOverride(self, config),
                !isPlayer && !isEntityOverride(self, config)
            );
        } catch (RuntimeException e) {
            return amount;
        }

        if (heldShieldReductionPercent == null || !heldShieldDr$rollPassiveBlock(self, config)) return amount;

        double finalReductionPercent = heldShieldDr$resolveDamageReductionPercent(self, heldShieldReductionPercent, config);
        float reducedAmount = amount * config.getDamageMultiplier(finalReductionPercent);

        if (config.shieldDurabilityDrainEnabled) {
            heldShieldDr$applyDurabilityDrain(self, amount - reducedAmount, config);
        }

        if (reducedAmount < amount) {
            heldShieldDr$playProcSound(self, config);
        }

        return reducedAmount;
    }

    private static boolean heldShieldDr$rollPassiveBlock(LivingEntity living, ShieldDrConfig config) {
        double chance = Math.max(0.0D, Math.min(1.0D, config.passiveBlockChance));
        return chance >= 1.0D || (chance > 0.0D && living.getRandom().nextDouble() < chance);
    }

    private static double heldShieldDr$resolveDamageReductionPercent(LivingEntity living, double basePercent, ShieldDrConfig config) {
        String mode = config.damageReductionMode;
        if (mode == null || !mode.trim().equalsIgnoreCase("random_range")) {
            return basePercent;
        }

        double min = Math.max(0.0D, Math.min(100.0D, config.damageReductionMinPercent));
        double max = Math.max(0.0D, Math.min(100.0D, config.damageReductionMaxPercent));
        if (max < min) {
            double swap = min;
            min = max;
            max = swap;
        }
        return min + living.getRandom().nextDouble() * (max - min);
    }

    private static void heldShieldDr$playProcSound(LivingEntity living, ShieldDrConfig config) {
        String rawId = config.procSoundId;
        if (rawId == null || rawId.trim().isEmpty()) {
            return;
        }

        long now = living.level().getGameTime();
        int cooldown = Math.max(0, config.procSoundCooldownTicks);
        UUID id = living.getUUID();
        Long last = heldShieldDr$lastProcSoundTicks.get(id);
        if (last != null && now - last < cooldown) {
            return;
        }

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(rawId.trim()));
        if (sound == null) {
            return;
        }

        heldShieldDr$lastProcSoundTicks.put(id, now);
        living.level().playSound(null, living.getX(), living.getY(), living.getZ(), sound, SoundSource.PLAYERS, (float) config.procSoundVolume, (float) config.procSoundPitch);
    }

    private static void heldShieldDr$applyDurabilityDrain(LivingEntity living, float damagePrevented, ShieldDrConfig config) {
        if (damagePrevented <= 0.0F) return;

        double rawDrain = damagePrevented * config.shieldDurabilityDrainMultiplier;
        double drain = (config.shieldDurabilityDrainCap > 0.0) ? Math.min(rawDrain, config.shieldDurabilityDrainCap) : rawDrain;
        int drainInt = Math.max(1, (int) Math.round(drain));

        InteractionHand shieldHand = null;
        ItemStack shieldStack = null;

        if (ShieldItemHelper.isShieldLike(living.getMainHandItem(), living)) {
            shieldHand = InteractionHand.MAIN_HAND;
            shieldStack = living.getMainHandItem();
        } else if (ShieldItemHelper.isShieldLike(living.getOffhandItem(), living)) {
            shieldHand = InteractionHand.OFF_HAND;
            shieldStack = living.getOffhandItem();
        }

        if (shieldStack == null || shieldStack.isEmpty()) return;

        final InteractionHand hand = shieldHand;
        shieldStack.hurtAndBreak(drainInt, living, e -> e.broadcastBreakEvent(hand));
    }

    private static Double getReductionPercent(LivingEntity entity, ShieldDrConfig config) {
        if (entity instanceof Player) {
            return config.applyToPlayers ? config.damageReductionPercent : null;
        }

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (key == null) return null;
        String entityId = key.toString();

        Map<String, Double> overrides = config.getEntityDamageReductionOverrides();
        if (overrides.containsKey(entityId)) return overrides.get(entityId);

        Set<String> whitelist = config.getEntityWhitelist();
        if (whitelist.contains(entityId)) return config.damageReductionPercent;

        return config.applyToAllEntities ? config.allEntitiesDamageReductionPercent : null;
    }

    private static boolean isEntityOverride(LivingEntity entity, ShieldDrConfig config) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (key == null) return false;
        return config.getEntityDamageReductionOverrides().containsKey(key.toString());
    }
}
