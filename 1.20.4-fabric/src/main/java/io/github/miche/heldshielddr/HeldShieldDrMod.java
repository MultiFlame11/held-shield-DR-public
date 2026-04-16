package io.github.miche.heldshielddr;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HeldShieldDrMod implements ModInitializer {
    public static final String MODID = "heldshielddr";
    public static final String NAME = "Held Shield DR";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static ShieldDrConfig config;

    @Override
    public void onInitialize() {
        config = ShieldDrConfig.load();
    }
}
