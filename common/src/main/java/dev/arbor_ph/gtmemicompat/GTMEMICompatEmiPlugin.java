package dev.arbor_ph.gtmemicompat;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public final class GTMEMICompatEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        GTEmiOreProcessingV2.register(registry);
    }
}
