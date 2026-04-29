package io.github.miche.heldshielddr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public final class ShieldDrConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("heldshielddr.json");

    public boolean enabled = true;
    public double damageReductionPercent = 33.0;
    public double passiveBlockChance = 1.0;
    public String damageReductionMode = "fixed";
    public double damageReductionMinPercent = 20.0;
    public double damageReductionMaxPercent = 33.0;
    public String procSoundId = "";
    public double procSoundVolume = 0.7;
    public double procSoundPitch = 1.0;
    public int procSoundCooldownTicks = 10;
    public boolean applyToPlayers = true;
    public boolean playersUseItemOverrides = true;
    public boolean showShieldTooltips = true;
    public boolean showItemOverrideText = false;
    public boolean applyToAllEntities = false;
    public double allEntitiesDamageReductionPercent = 33.0;
    public boolean entitiesUseItemOverrides = false;
    public List<String> entityWhitelist = Arrays.asList("touhou_little_maid:entity.passive.maid");
    public List<String> entityDamageReductionOverrides = Arrays.asList("touhou_little_maid:entity.passive.maid;65.0");
    public List<String> itemDamageReductionOverrides = Collections.emptyList();
    public boolean shieldDurabilityDrainEnabled = false;
    public double shieldDurabilityDrainMultiplier = 1.0;
    public double shieldDurabilityDrainCap = 0.0;

    private transient Set<String> cachedEntityWhitelist;
    private transient Map<String, Double> cachedEntityDamageReductionOverrides;
    private transient Map<String, Double> cachedItemDamageReductionOverrides;

    public static ShieldDrConfig load() {
        ShieldDrConfig config = null;
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                config = GSON.fromJson(reader, ShieldDrConfig.class);
            } catch (IOException ignored) {
            }
        }

        if (config == null) {
            config = new ShieldDrConfig();
        }

        config.buildCaches();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public Set<String> getEntityWhitelist() {
        return cachedEntityWhitelist;
    }

    public Map<String, Double> getEntityDamageReductionOverrides() {
        return cachedEntityDamageReductionOverrides;
    }

    public Map<String, Double> getItemDamageReductionOverrides() {
        return cachedItemDamageReductionOverrides;
    }

    public float getDamageMultiplier(double reductionPercent) {
        double reduction = Math.max(0.0D, Math.min(100.0D, reductionPercent));
        return (float) (1.0D - (reduction / 100.0D));
    }

    private void buildCaches() {
        cachedEntityWhitelist = parseStringSet(entityWhitelist);
        cachedEntityDamageReductionOverrides = parseOverrides(entityDamageReductionOverrides, "entity");
        cachedItemDamageReductionOverrides = parseOverrides(itemDamageReductionOverrides, "item");
    }

    private static Set<String> parseStringSet(List<String> list) {
        if (list == null || list.isEmpty()) return Collections.emptySet();
        Set<String> set = new HashSet<>();
        for (String entry : list) {
            if (entry != null && !entry.trim().isEmpty()) set.add(entry.trim());
        }
        return Collections.unmodifiableSet(set);
    }

    private static Map<String, Double> parseOverrides(List<String> list, String label) {
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        Map<String, Double> map = new HashMap<>();
        for (String entry : list) {
            if (entry == null) continue;
            String[] parts = entry.split(";", 2);
            if (parts.length != 2 || parts[0].trim().isEmpty()) continue;
            try {
                map.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
            } catch (NumberFormatException ignored) {
                HeldShieldDrMod.LOGGER.warn("Ignoring malformed {} override entry: {}", label, entry);
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
