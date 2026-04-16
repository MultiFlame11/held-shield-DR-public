package io.github.miche.heldshielddr;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(HeldShieldDrMod.MODID)
public class HeldShieldDrMod {
    public static final String MODID = "heldshielddr";
    public static final String NAME = "Held Shield DR";
    public static final String VERSION = "1.1.2";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public HeldShieldDrMod(IEventBus modBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ShieldDrConfig.SPEC);
        modBus.addListener(ShieldDrConfig::onConfigLoad);
        modBus.addListener(ShieldDrConfig::onConfigReload);
        NeoForge.EVENT_BUS.register(new ShieldDamageHandler());
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(new ShieldTooltipHandler());
        }
    }
}
