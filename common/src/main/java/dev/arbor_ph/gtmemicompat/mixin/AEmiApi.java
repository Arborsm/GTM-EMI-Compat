package dev.arbor_ph.gtmemicompat.mixin;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(value = EmiApi.class,remap = false)
public interface AEmiApi {
    @Invoker
    static void invokeSetPages(Map<EmiRecipeCategory, List<EmiRecipe>> recipes, EmiIngredient stack) {

    }
}
