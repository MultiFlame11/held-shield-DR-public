package io.github.miche.heldshielddr.mixin;

import io.github.miche.heldshielddr.HeldShieldDrMod;
import io.github.miche.heldshielddr.ShieldDrConfig;
import io.github.miche.heldshielddr.ShieldItemHelper;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyArg(
        method = "hurt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            ordinal = 0
        ),
        index = 1
    )
    private float heldShieldDr$modifyDamageBeforeActuallyHurtFirst(float amount) {
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
    private float heldShieldDr$modifyDamageBeforeActuallyHurtSecond(float amount) {
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

        if (heldShieldReductionPercent == null) return amount;

        float finalAmount = amount * config.getDamageMultiplier(heldShieldReductionPercent);

        if (config.shieldDurabilityDrainEnabled) {
            heldShieldDr$applyDurabilityDrain(self, amount - finalAmount, config);
        }

        return finalAmount;
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
