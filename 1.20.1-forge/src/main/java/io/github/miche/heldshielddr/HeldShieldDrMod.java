package io.github.miche.heldshielddr;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(HeldShieldDrMod.MODID)
public class HeldShieldDrMod {
    public static final String MODID = "heldshielddr";
    public static final String NAME = "Held Shield DR";
    public static final String VERSION = "1.1.1";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public HeldShieldDrMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ShieldDrConfig.SPEC);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(ShieldDrConfig::onConfigLoad);
        modBus.addListener(ShieldDrConfig::onConfigReload);

        MinecraftForge.EVENT_BUS.register(new ShieldDamageHandler());
        MinecraftForge.EVENT_BUS.register(new ShieldTooltipHandler());
    }
}
