package io.github.miche.heldshielddr;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

public final class HeldShieldDrClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register(new ShieldTooltipHandler());
    }
}
