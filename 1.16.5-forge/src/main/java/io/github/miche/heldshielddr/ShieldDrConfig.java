package io.github.miche.heldshielddr;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public class ShieldDrConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_REDUCTION_PERCENT;
    public static final ForgeConfigSpec.DoubleValue PASSIVE_BLOCK_CHANCE;
    public static final ForgeConfigSpec.ConfigValue<String> DAMAGE_REDUCTION_MODE;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_REDUCTION_MIN_PERCENT;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_REDUCTION_MAX_PERCENT;
    public static final ForgeConfigSpec.ConfigValue<String> PROC_SOUND_ID;
    public static final ForgeConfigSpec.DoubleValue PROC_SOUND_VOLUME;
    public static final ForgeConfigSpec.DoubleValue PROC_SOUND_PITCH;
    public static final ForgeConfigSpec.IntValue PROC_SOUND_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue APPLY_TO_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue PLAYERS_USE_ITEM_OVERRIDES;
    public static final ForgeConfigSpec.BooleanValue SHOW_SHIELD_TOOLTIPS;
    public static final ForgeConfigSpec.BooleanValue SHOW_ITEM_OVERRIDE_TEXT;
    public static final ForgeConfigSpec.BooleanValue APPLY_TO_ALL_ENTITIES;
    public static final ForgeConfigSpec.DoubleValue ALL_ENTITIES_DAMAGE_REDUCTION_PERCENT;
    public static final ForgeConfigSpec.BooleanValue ENTITIES_USE_ITEM_OVERRIDES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_WHITELIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_DAMAGE_REDUCTION_OVERRIDES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_DAMAGE_REDUCTION_OVERRIDES;

    public static final ForgeConfigSpec.BooleanValue SHIELD_DURABILITY_DRAIN_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SHIELD_DURABILITY_DRAIN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SHIELD_DURABILITY_DRAIN_CAP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        ENABLED = builder
            .comment("Set to false to disable the held shield DR effect entirely.")
            .define("enabled", true);

        DAMAGE_REDUCTION_PERCENT = builder
            .comment(
                "Damage reduction percentage used by players when applyToPlayers is enabled.",
                "This is also used by non-player entities listed in entityWhitelist.",
                "Per-entity overrides only affect non-player entities and take priority over this value for those entities."
            )
            .defineInRange("damageReductionPercent", 33.0, 0.0, 100.0);
        PASSIVE_BLOCK_CHANCE = builder
            .comment("Chance that passive held shield DR triggers on a valid hit. 1.0 = always, 0.25 = 25 percent.")
            .defineInRange("passiveBlockChance", 1.0, 0.0, 1.0);

        DAMAGE_REDUCTION_MODE = builder
            .comment("How passive shield DR is picked. fixed uses the normal DR value. random_range rolls between damageReductionMinPercent and damageReductionMaxPercent.")
            .define("damageReductionMode", "fixed");

        DAMAGE_REDUCTION_MIN_PERCENT = builder
            .comment("Minimum DR percent used when damageReductionMode is random_range.")
            .defineInRange("damageReductionMinPercent", 20.0, 0.0, 100.0);

        DAMAGE_REDUCTION_MAX_PERCENT = builder
            .comment("Maximum DR percent used when damageReductionMode is random_range.")
            .defineInRange("damageReductionMaxPercent", 33.0, 0.0, 100.0);

        PROC_SOUND_ID = builder
            .comment("Optional sound id played when passive shield DR triggers. Blank disables it. Example: minecraft:item.shield.block")
            .define("procSoundId", "");

        PROC_SOUND_VOLUME = builder
            .comment("Volume for procSoundId.")
            .defineInRange("procSoundVolume", 0.7, 0.0, 4.0);

        PROC_SOUND_PITCH = builder
            .comment("Pitch for procSoundId.")
            .defineInRange("procSoundPitch", 1.0, 0.1, 4.0);

        PROC_SOUND_COOLDOWN_TICKS = builder
            .comment("Minimum ticks between proc sounds for the same entity.")
            .defineInRange("procSoundCooldownTicks", 10, 0, 1200);

        APPLY_TO_PLAYERS = builder
            .comment(
                "If true, players can benefit from held shield damage reduction using damageReductionPercent.",
                "Players do not use entityWhitelist, entityDamageReductionOverrides, or all-entities fallback settings."
            )
            .define("applyToPlayers", true);

        PLAYERS_USE_ITEM_OVERRIDES = builder
            .comment(
                "If true, players can use itemDamageReductionOverrides when holding a matching shield-like item.",
                "If false, players always use their normal damageReductionPercent instead."
            )
            .define("playersUseItemOverrides", true);

        SHOW_SHIELD_TOOLTIPS = builder
            .comment(
                "If true, shield-like items will show a tooltip line describing their passive shield DR behavior.",
                "Enabled by default so item-specific overrides are visible without extra setup."
            )
            .define("showShieldTooltips", true);

        SHOW_ITEM_OVERRIDE_TEXT = builder
            .comment("If true, item-specific tooltip lines will append an '(Item Override)' suffix when a shield uses an item override.")
            .define("showItemOverrideText", false);

        APPLY_TO_ALL_ENTITIES = builder
            .comment(
                "If true, non-player living entities not covered by entityWhitelist can also benefit from held shield damage reduction.",
                "Per-entity overrides still take priority."
            )
            .define("applyToAllEntities", false);

        ALL_ENTITIES_DAMAGE_REDUCTION_PERCENT = builder
            .comment(
                "Damage reduction percentage used for non-player living entities covered by applyToAllEntities.",
                "This is only used when an entity does not have a per-entity override and is not in entityWhitelist."
            )
            .defineInRange("allEntitiesDamageReductionPercent", 33.0, 0.0, 100.0);

        ENTITIES_USE_ITEM_OVERRIDES = builder
            .comment(
                "If true, eligible non-player entities can use itemDamageReductionOverrides when holding a matching shield-like item.",
                "Entities with their own per-entity override keep that explicit value and ignore item overrides.",
                "Entities using entityWhitelist or applyToAllEntities keep the higher of their normal DR or the held item's override.",
                "This setting does not make an entity eligible by itself; the entity still needs a per-entity override, entityWhitelist entry, or applyToAllEntities."
            )
            .define("entitiesUseItemOverrides", false);

        ENTITY_WHITELIST = builder
            .comment(
                "Exact non-player entity ids that should use the shared damageReductionPercent while holding a shield.",
                "Useful when applyToAllEntities is disabled, or when specific entities should use the shared value instead of the all-entities fallback.",
                "Example: touhou_little_maid:entity.passive.maid"
            )
            .defineList("entityWhitelist",
                Arrays.asList("touhou_little_maid:entity.passive.maid"),
                obj -> obj instanceof String);

        ENTITY_DAMAGE_REDUCTION_OVERRIDES = builder
            .comment(
                "Optional per-entity custom damage reduction values in the form entity_id;percent.",
                "Overrides use their own custom value instead of the shared damageReductionPercent and take priority over both entityWhitelist and applyToAllEntities.",
                "Entities listed here do not also need to be added to entityWhitelist.",
                "Example: touhou_little_maid:entity.passive.maid;65.0"
            )
            .defineList("entityDamageReductionOverrides",
                Arrays.asList("touhou_little_maid:entity.passive.maid;65.0"),
                obj -> obj instanceof String);

        ITEM_DAMAGE_REDUCTION_OVERRIDES = builder
            .comment(
                "Optional per-item custom damage reduction values for shield-like items in the form item_id;percent.",
                "Players only use these when playersUseItemOverrides is enabled.",
                "Eligible non-player entities only use these when entitiesUseItemOverrides is enabled.",
                "Non-player entities with their own per-entity override ignore item overrides, while entities using shared or all-entities DR keep the higher value.",
                "Example: basemetals:diamond_shield;90.0"
            )
            .defineList("itemDamageReductionOverrides",
                Collections.emptyList(),
                obj -> obj instanceof String);

        SHIELD_DURABILITY_DRAIN_ENABLED = builder
            .comment("If true, passively blocking damage with a held shield will drain its durability based on how much damage was prevented.")
            .define("shieldDurabilityDrainEnabled", false);

        SHIELD_DURABILITY_DRAIN_MULTIPLIER = builder
            .comment(
                "How much durability to drain per point of damage prevented by the passive DR.",
                "1.0 = drain exactly as much durability as damage prevented. 0.5 = half that. 2.0 = double."
            )
            .defineInRange("shieldDurabilityDrainMultiplier", 1.0, 0.0, 100.0);

        SHIELD_DURABILITY_DRAIN_CAP = builder
            .comment(
                "Maximum durability drained per hit regardless of damage prevented.",
                "Set to 0 to disable the cap entirely."
            )
            .defineInRange("shieldDurabilityDrainCap", 0.0, 0.0, 10000.0);

        builder.pop();
        SPEC = builder.build();
    }

    private static volatile Set<String> cachedEntityWhitelist = Collections.emptySet();
    private static volatile Map<String, Double> cachedEntityDamageReductionOverrides = Collections.emptyMap();
    private static volatile Map<String, Double> cachedItemDamageReductionOverrides = Collections.emptyMap();

    public static void onConfigLoad(ModConfig.Loading event) {
        if (event.getConfig().getModId().equals(HeldShieldDrMod.MODID)) {
            refreshCaches();
        }
    }

    public static void onConfigReload(ModConfig.Reloading event) {
        if (event.getConfig().getModId().equals(HeldShieldDrMod.MODID)) {
            refreshCaches();
        }
    }

    public static void refreshCaches() {
        cachedEntityWhitelist = parseEntityWhitelist();
        cachedEntityDamageReductionOverrides = parseEntityDamageReductionOverrides();
        cachedItemDamageReductionOverrides = parseItemDamageReductionOverrides();
    }

    public static Set<String> getEntityWhitelist() {
        return cachedEntityWhitelist;
    }

    public static Map<String, Double> getEntityDamageReductionOverrides() {
        return cachedEntityDamageReductionOverrides;
    }

    public static Map<String, Double> getItemDamageReductionOverrides() {
        return cachedItemDamageReductionOverrides;
    }

    public static float getDamageMultiplier() {
        if (!ENABLED.get()) {
            return 1.0F;
        }

        return getDamageMultiplier(DAMAGE_REDUCTION_PERCENT.get());
    }

    public static float getDamageMultiplier(double reductionPercent) {
        if (!ENABLED.get()) {
            return 1.0F;
        }

        double reduction = Math.max(0.0D, Math.min(100.0D, reductionPercent));
        return (float) (1.0D - (reduction / 100.0D));
    }

    private static Set<String> parseEntityWhitelist() {
        List<? extends String> list = ENTITY_WHITELIST.get();
        if (list == null || list.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> whitelist = new HashSet<>();
        for (String entry : list) {
            if (entry == null) {
                continue;
            }

            String entityId = entry.trim();
            if (!entityId.isEmpty()) {
                whitelist.add(entityId);
            }
        }
        return whitelist.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(whitelist);
    }

    private static Map<String, Double> parseEntityDamageReductionOverrides() {
        return parseDamageReductionOverrides(
            ENTITY_DAMAGE_REDUCTION_OVERRIDES.get(),
            "entity damage reduction override",
            "entity_id;percent"
        );
    }

    private static Map<String, Double> parseItemDamageReductionOverrides() {
        return parseDamageReductionOverrides(
            ITEM_DAMAGE_REDUCTION_OVERRIDES.get(),
            "item damage reduction override",
            "item_id;percent"
        );
    }

    private static Map<String, Double> parseDamageReductionOverrides(List<? extends String> entries, String entryLabel, String formatExample) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> overrides = new HashMap<>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }

            String[] parts = entry.split(";", 2);
            if (parts.length != 2) {
                HeldShieldDrMod.LOGGER.warn("Ignoring malformed {} entry '{}'. Expected format: {}", entryLabel, entry, formatExample);
                continue;
            }

            String id = parts[0].trim();
            String percentText = parts[1].trim();
            if (id.isEmpty()) {
                continue;
            }

            try {
                overrides.put(id, Double.parseDouble(percentText));
            } catch (NumberFormatException ignored) {
                HeldShieldDrMod.LOGGER.warn("Ignoring {} '{}' because '{}' is not a valid number.", entryLabel, entry, percentText);
            }
        }
        return overrides.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(overrides);
    }
}
