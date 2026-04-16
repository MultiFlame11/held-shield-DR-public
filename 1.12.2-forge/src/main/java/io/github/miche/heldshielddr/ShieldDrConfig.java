package io.github.miche.heldshielddr;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = HeldShieldDrMod.MODID)
public class ShieldDrConfig {
    private static volatile Set<String> cachedEntityWhitelist = Collections.emptySet();
    private static volatile Map<String, Double> cachedEntityDamageReductionOverrides = Collections.emptyMap();
    private static volatile Map<String, Double> cachedItemDamageReductionOverrides = Collections.emptyMap();

    @Config.Comment("Set to false to disable the held shield DR effect entirely.")
    public static boolean enabled = true;

    @Config.Comment({
        "Damage reduction percentage used by players when applyToPlayers is enabled.",
        "This is also used by non-player entities listed in entityWhitelist.",
        "Per-entity overrides only affect non-player entities and take priority over this value for those entities."
    })
    @Config.RangeDouble(min = 0.0D, max = 100.0D)
    public static double damageReductionPercent = 33.0D;

    @Config.Comment({
        "If true, players can benefit from held shield damage reduction using damageReductionPercent.",
        "Players do not use entityWhitelist, entityDamageReductionOverrides, or all-entities fallback settings."
    })
    public static boolean applyToPlayers = true;

    @Config.Comment({
        "If true, players can use itemDamageReductionOverrides when holding a matching shield-like item.",
        "If false, players always use their normal damageReductionPercent instead."
    })
    public static boolean playersUseItemOverrides = true;

    @Config.Comment({
        "If true, shield-like items will show a tooltip line describing their passive shield DR behavior.",
        "Enabled by default so item-specific overrides are visible without extra setup."
    })
    public static boolean showShieldTooltips = true;

    @Config.Comment("If true, item-specific tooltip lines will append an '(Item Override)' suffix when a shield uses an item override.")
    public static boolean showItemOverrideText = false;

    @Config.Comment({
        "If true, non-player living entities not covered by entityWhitelist can also benefit from held shield damage reduction.",
        "Per-entity overrides still take priority."
    })
    public static boolean applyToAllEntities = false;

    @Config.Comment({
        "Damage reduction percentage used for non-player living entities covered by applyToAllEntities.",
        "This is only used when an entity does not have a per-entity override and is not in entityWhitelist."
    })
    @Config.RangeDouble(min = 0.0D, max = 100.0D)
    public static double allEntitiesDamageReductionPercent = 33.0D;

    @Config.Comment({
        "Exact non-player entity ids that should use the shared damageReductionPercent while holding a shield.",
        "Useful when applyToAllEntities is disabled, or when specific entities should use the shared value instead of the all-entities fallback.",
        "Example: touhou_little_maid:entity.passive.maid"
    })
    public static String[] entityWhitelist = new String[] {
        "touhou_little_maid:entity.passive.maid"
    };

    @Config.Comment({
        "Optional per-entity custom damage reduction values in the form entity_id;percent.",
        "Overrides use their own custom value instead of the shared damageReductionPercent and take priority over both entityWhitelist and applyToAllEntities.",
        "Entities listed here do not also need to be added to entityWhitelist.",
        "Example: touhou_little_maid:entity.passive.maid;65.0"
    })
    public static String[] entityDamageReductionOverrides = new String[] {
        "touhou_little_maid:entity.passive.maid;65.0"
    };

    @Config.Comment({
        "If true, eligible non-player entities can use itemDamageReductionOverrides when holding a matching shield-like item.",
        "Entities with their own per-entity override keep that explicit value and ignore item overrides.",
        "Entities using entityWhitelist or applyToAllEntities keep the higher of their normal DR or the held item's override.",
        "This setting does not make an entity eligible by itself; the entity still needs a per-entity override, entityWhitelist entry, or applyToAllEntities."
    })
    public static boolean entitiesUseItemOverrides = false;

    @Config.Comment({
        "Optional per-item custom damage reduction values for shield-like items in the form item_id;percent.",
        "Players only use these when playersUseItemOverrides is enabled.",
        "Eligible non-player entities only use these when entitiesUseItemOverrides is enabled.",
        "Non-player entities with their own per-entity override ignore item overrides, while entities using shared or all-entities DR keep the higher value.",
        "Example: basemetals:diamond_shield;90.0"
    })
    public static String[] itemDamageReductionOverrides = new String[0];

    @Config.Comment("If true, passively blocking damage with a held shield will drain its durability based on how much damage was prevented.")
    public static boolean shieldDurabilityDrainEnabled = false;

    @Config.Comment({
        "How much durability to drain per point of damage prevented by the passive DR.",
        "1.0 = drain exactly as much durability as damage prevented. 0.5 = half that. 2.0 = double."
    })
    @Config.RangeDouble(min = 0.0D, max = 100.0D)
    public static double shieldDurabilityDrainMultiplier = 1.0D;

    @Config.Comment({
        "Maximum durability drained per hit regardless of damage prevented.",
        "Set to 0 to disable the cap entirely."
    })
    @Config.RangeDouble(min = 0.0D, max = 10000.0D)
    public static double shieldDurabilityDrainCap = 0.0D;

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
        if (!enabled) {
            return 1.0F;
        }

        return getDamageMultiplier(damageReductionPercent);
    }

    public static float getDamageMultiplier(double reductionPercent) {
        if (!enabled) {
            return 1.0F;
        }

        double reduction = Math.max(0.0D, Math.min(100.0D, reductionPercent));
        return (float) (1.0D - (reduction / 100.0D));
    }

    private static Set<String> parseEntityWhitelist() {
        if (entityWhitelist == null || entityWhitelist.length == 0) {
            return Collections.emptySet();
        }

        Set<String> whitelist = new HashSet<>();
        for (String entry : entityWhitelist) {
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
            entityDamageReductionOverrides,
            "entity damage reduction override",
            "entity_id;percent"
        );
    }

    private static Map<String, Double> parseItemDamageReductionOverrides() {
        return parseDamageReductionOverrides(
            itemDamageReductionOverrides,
            "item damage reduction override",
            "item_id;percent"
        );
    }

    private static Map<String, Double> parseDamageReductionOverrides(String[] entries, String entryLabel, String formatExample) {
        if (entries == null || entries.length == 0) {
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

    static {
        refreshCaches();
    }

    @Mod.EventBusSubscriber(modid = HeldShieldDrMod.MODID)
    private static class ConfigEvents {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (HeldShieldDrMod.MODID.equals(event.getModID())) {
                ConfigManager.sync(HeldShieldDrMod.MODID, Config.Type.INSTANCE);
                refreshCaches();
            }
        }
    }
}
