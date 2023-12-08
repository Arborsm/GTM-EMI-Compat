package dev.arbor_ph.gtmemicompat.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.arbor_ph.gtmemicompat.GTMEMICompat;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GTMEMICompat.MOD_ID)
public class GTMEMICompatForge {
    public GTMEMICompatForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(GTMEMICompat.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        GTMEMICompat.init();
    }
}