package io.github.miche.heldshielddr;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class HeldShieldDrMod implements ModInitializer {
    public static final String MODID = "heldshielddr";
    public static final String NAME = "Held Shield DR";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static ShieldDrConfig config;

    @Override
    public void onInitialize() {
        config = ShieldDrConfig.load();
    }
}
