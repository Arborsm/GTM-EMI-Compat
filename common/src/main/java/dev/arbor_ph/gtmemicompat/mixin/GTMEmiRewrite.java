package dev.arbor_ph.gtmemicompat.mixin;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.integration.emi.GTEMIPlugin;
import com.gregtechceu.gtceu.integration.emi.multipage.MultiblockInfoEmiCategory;
import com.gregtechceu.gtceu.integration.emi.oreprocessing.GTOreProcessingEmiCategory;
import dev.arbor_ph.gtmemicompat.GTEmiOreProcessingV2;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = GTEMIPlugin.class, remap = false)
public class GTMEmiRewrite {
    /**
     * @author Arbor
     * @reason 重写
     */
    @Overwrite
    public void register(EmiRegistry registry) {
        registry.addCategory(GTOreProcessingEmiCategory.CATEGORY);
        registry.addCategory(MultiblockInfoEmiCategory.CATEGORY);
        // recipes
        MultiblockInfoEmiCategory.registerDisplays(registry);
        // workstations
        MultiblockInfoEmiCategory.registerWorkStations(registry);
        // workstations
        for (MachineDefinition definition : GTMachines.ELECTRIC_FURNACE) {
            if (definition != null) {
                registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(definition.asStack()));
            }
        }
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(GTMachines.STEAM_FURNACE.left().asStack()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(GTMachines.STEAM_FURNACE.right().asStack()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(GTMachines.STEAM_OVEN.asStack()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(GTMachines.MULTI_SMELTER.asStack()));

        // rewrite
        GTEmiOreProcessingV2.register(registry);
    }
}
