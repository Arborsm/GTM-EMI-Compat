package dev.arbor_ph.gtmemicompat.mixin;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.emi.recipe.GTRecipeTypeEmiCategory;
import dev.arbor_ph.gtmemicompat.GTMEMICompatEmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = GTRecipeTypeEmiCategory.class, remap = false)
class MGTRecipeTypeEmiCategory {
    /**
     @author Phoupraw
     @reason 必要时冲突
     */
    @Overwrite
    public static void registerWorkStations(EmiRegistry registry) {
        for (GTRecipeType thisType : GTRegistries.RECIPE_TYPES) {
            registry.addWorkstation(GTRecipeTypeEmiCategory.CATEGORIES.apply(thisType), EmiStack.of(GTMEMICompatEmiPlugin.RECIPE_MACHINES.get(thisType).last().asStack()));
            //MachineDefinition highest = null;
            //for (MachineDefinition machine : GTRegistries.MACHINES) {
            //    GTRecipeType[] recipeTypes = machine.getRecipeTypes();
            //    if (recipeTypes == null) continue;
            //    for (GTRecipeType thatType : recipeTypes) {
            //        if (thatType == thisType && (highest == null || highest.getTier() < machine.getTier())) {
            //            highest = machine;
            //        }
            //    }
            //}
            //if (highest != null) {
            //    registry.addWorkstation(GTRecipeTypeEmiCategory.CATEGORIES.apply(thisType), EmiStack.of(highest.asStack()));
            //}
        }
    }
}
