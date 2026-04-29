package io.github.miche.heldshielddr;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ShieldDamageHandler {
    private static final Map<UUID, Long> LAST_PROC_SOUND_TICKS = new ConcurrentHashMap<UUID, Long>();
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

        if (ShieldDrConfig.shieldDurabilityDrainEnabled) {
            applyDurabilityDrain(living, originalAmount - reducedAmount);
        }
    }
    private static boolean rollPassiveBlock(EntityLivingBase living) {
        double chance = Math.max(0.0D, Math.min(1.0D, ShieldDrConfig.passiveBlockChance));
        return chance >= 1.0D || (chance > 0.0D && living.getRNG().nextDouble() < chance);
    }

    private static double resolveDamageReductionPercent(EntityLivingBase living, double basePercent) {
        String mode = ShieldDrConfig.damageReductionMode;
        if (mode == null || !mode.trim().equalsIgnoreCase("random_range")) {
            return basePercent;
        }

        double min = Math.max(0.0D, Math.min(100.0D, ShieldDrConfig.damageReductionMinPercent));
        double max = Math.max(0.0D, Math.min(100.0D, ShieldDrConfig.damageReductionMaxPercent));
        if (max < min) {
            double swap = min;
            min = max;
            max = swap;
        }
        return min + living.getRNG().nextDouble() * (max - min);
    }

    private static void playProcSound(EntityLivingBase living) {
        String rawId = ShieldDrConfig.procSoundId;
        if (rawId == null || rawId.trim().isEmpty()) {
            return;
        }

        long now = living.world.getTotalWorldTime();
        int cooldown = Math.max(0, ShieldDrConfig.procSoundCooldownTicks);
        UUID id = living.getUniqueID();
        Long last = LAST_PROC_SOUND_TICKS.get(id);
        if (last != null && now - last < cooldown) {
            return;
        }

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(rawId.trim()));
        if (sound == null) {
            return;
        }

        LAST_PROC_SOUND_TICKS.put(id, now);
        living.world.playSound(null, living.posX, living.posY, living.posZ, sound, SoundCategory.PLAYERS, (float) ShieldDrConfig.procSoundVolume, (float) ShieldDrConfig.procSoundPitch);
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
