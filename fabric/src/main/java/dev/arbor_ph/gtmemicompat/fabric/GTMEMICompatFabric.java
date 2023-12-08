package dev.arbor_ph.gtmemicompat.fabric;

import dev.arbor_ph.gtmemicompat.GTMEMICompat;
import net.fabricmc.api.ModInitializer;

public class GTMEMICompatFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        GTMEMICompat.init();
    }
}