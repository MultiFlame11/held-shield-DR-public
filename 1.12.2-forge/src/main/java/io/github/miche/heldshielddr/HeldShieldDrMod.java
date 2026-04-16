package io.github.miche.heldshielddr;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = HeldShieldDrMod.MODID,
    name = HeldShieldDrMod.NAME,
    version = HeldShieldDrMod.VERSION,
    acceptableRemoteVersions = "*"
)
public class HeldShieldDrMod {
    public static final String MODID = "heldshielddr";
    public static final String NAME = "Held Shield DR";
    public static final String VERSION = "1.1.2";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ConfigManager.sync(MODID, Config.Type.INSTANCE);
        ShieldDrConfig.refreshCaches();
        MinecraftForge.EVENT_BUS.register(new ShieldDamageHandler());
        if (event.getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new ShieldTooltipHandler());
        }
    }
}
