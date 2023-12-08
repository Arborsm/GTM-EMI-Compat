package dev.arbor_ph.gtmemicompat.mixin.fabric;

import com.gregtechceu.gtceu.integration.emi.oreprocessing.GTOreProcessingEmiCategory;
import dev.emi.emi.api.EmiRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(value = GTOreProcessingEmiCategory.class,remap = false)
class MGTOreProcessingEmiCategory {
    /**
     * @author Phoupraw
     * @reason 用于产生必要的冲突
     */
    @Overwrite
    public static void registerWorkStations(EmiRegistry registry) {

    }
}