package dev.arbor_ph.gtmemicompat.fabric;

import dev.arbor_ph.gtmemicompat.GTEmiOreProcessingV2;
import dev.arbor_ph.gtmemicompat.GTMEMICompat;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;

@Environment(EnvType.CLIENT)
public final class GTMEMICompatFabric implements ModInitializer, ClientModInitializer, EmiPlugin {
    @Override
    public void onInitialize() {
        GTMEMICompat.init();
    }
    @Override
    public void register(EmiRegistry registry) {

    }
    @Override
    public void onInitializeClient() {

    }
}