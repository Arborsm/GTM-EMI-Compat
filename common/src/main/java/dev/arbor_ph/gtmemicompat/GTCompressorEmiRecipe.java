package dev.arbor_ph.gtmemicompat;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.emi.recipe.GTRecipeTypeEmiCategory;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GTCompressorEmiRecipe implements EmiRecipe {
    public static final int MAX_WIDTH = 134;
    public static SlotWidget addSlot(WidgetHolder widgets, EmiIngredient stack, int x, int y) {
        if (stack.getEmiStacks().stream().map(EmiStack::getKey).allMatch(Fluid.class::isInstance)) {
            return GTEmiOreProcessingV2.addTank(widgets, stack, x, y);
        }
        return widgets.addSlot(stack, x, y);
    }
    //public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory()
    public final GTRecipe gtRecipe;
    protected final List<EmiIngredient> inputs = new ArrayList<>();
    protected final List<EmiStack> outputs = new ArrayList<>();
    protected final List<EmiIngredient> catalysts = new ArrayList<>();
    public GTCompressorEmiRecipe(GTRecipe gtRecipe) {
        this.gtRecipe = gtRecipe;
        for (Map.Entry<RecipeCapability<?>, List<Content>> entry : gtRecipe.inputs.entrySet()) {
            RecipeCapability<?> capability = entry.getKey();
            List<Content> contents = entry.getValue();
            if (capability instanceof ItemRecipeCapability) {
                for (Content content : contents) {
                    if (content.content instanceof Ingredient ingredient) {
                        inputs.add(EmiIngredient.of(ingredient, ingredient.getItems()[0].getCount()));
                    }
                }
            } else if (capability instanceof FluidRecipeCapability) {
                for (Content content : contents) {
                    List<EmiIngredient> emiStacks = new ArrayList<>();
                    if (content.content instanceof FluidIngredient fluidIngredient) {
                        for (FluidStack fluidStack : fluidIngredient.getStacks()) {
                            emiStacks.add(EmiStack.of(fluidStack.getFluid(), fluidStack.getTag(), fluidStack.getAmount()));
                        }
                    }
                    inputs.add(EmiIngredient.of(emiStacks));
                }
            }
        }
        for (Map.Entry<RecipeCapability<?>, List<Content>> entry : gtRecipe.outputs.entrySet()) {
            RecipeCapability<?> capability = entry.getKey();
            List<Content> contents = entry.getValue();
            if (capability instanceof ItemRecipeCapability) {
                for (Content content : contents) {
                    if (content.content instanceof Ingredient ingredient) {
                        for (ItemStack itemStack : ingredient.getItems()) {
                            outputs.add(EmiStack.of(itemStack));
                        }
                    }
                }
            } else if (capability instanceof FluidRecipeCapability) {
                for (Content content : contents) {
                    if (content.content instanceof FluidIngredient fluidIngredient) {
                        for (FluidStack fluidStack : fluidIngredient.getStacks()) {
                            outputs.add(EmiStack.of(fluidStack.getFluid(), fluidStack.getTag(), fluidStack.getAmount()));
                        }
                    }
                }
            }
        }
        int recipeTier = RecipeHelper.getRecipeEUtTier(gtRecipe);
        for (MachineDefinition machine : GTRegistries.MACHINES) {
            GTRecipeType[] recipeTypes = machine.getRecipeTypes();
            if (recipeTypes == null) continue;
            for (GTRecipeType thatType : recipeTypes) {
                if (thatType == gtRecipe.recipeType && machine.getTier() >= recipeTier) {
                    catalysts.add(EmiStack.of(machine.asStack()));
                }
            }
        }
        GTMEMICompatEmiPlugin.normalizeCatalysts(catalysts);
    }
    @Override
    public EmiRecipeCategory getCategory() {
        return GTRecipeTypeEmiCategory.CATEGORIES.apply(gtRecipe.recipeType);
    }
    @Override
    public @NotNull ResourceLocation getId() {
        return gtRecipe.getId();
    }
    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }
    @Override
    public List<EmiIngredient> getCatalysts() {
        return catalysts;
    }
    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }
    @Override
    public int getDisplayWidth() {
        Font textRenderer = Minecraft.getInstance().font;
        int eUtTier = RecipeHelper.getRecipeEUtTier(gtRecipe);
        String tierText = GTValues.VNF[eUtTier];
        int textWidth = textRenderer.width(tierText);
        int x = Math.max(EmiTexture.SLOT.width, textWidth);
        int maxWidth = MAX_WIDTH - x - EmiTexture.FULL_ARROW.width;
        int maxRowSize = maxWidth / EmiTexture.SLOT.width;
        int inputsSize = getInputs().size();
        int outputsSize = getOutputs().size();
        int rowSize = Math.min(inputsSize + outputsSize, maxRowSize);
        int duration = gtRecipe.duration;
        List<Component> texts = new ArrayList<>(4);
        texts.add(EmiPort.translatable("emi.cooking.time", DecimalFormat.getInstance().format(duration / 20.0)));
        long inputEUt = RecipeHelper.getInputEUt(gtRecipe);
        if (inputEUt != 0) {
            texts.add(Component.literal("%d EU".formatted(inputEUt * duration)));
            texts.add(Component.literal("-%d EU/t".formatted(inputEUt)));
        }
        long outputEUt = RecipeHelper.getOutputEUt(gtRecipe);
        if (outputEUt != 0) {
            texts.add(Component.literal("%d EU".formatted(outputEUt * duration)));
            texts.add(Component.literal("+%d EU/t".formatted(outputEUt)));
        }
        int middleWidth = Math.max(EmiTexture.FULL_ARROW.width, texts.stream().mapToInt(textRenderer::width).max().orElseThrow());
        return x + middleWidth + rowSize * EmiTexture.SLOT.width;
    }
    @Override
    public int getDisplayHeight() {
        Font textRenderer = Minecraft.getInstance().font;
        int textHeight = textRenderer.lineHeight;
        String tierText = GTValues.VNF[RecipeHelper.getRecipeEUtTier(gtRecipe)];
        int textWidth = textRenderer.width(tierText);
        int x = Math.max(EmiTexture.SLOT.width, textWidth);
        int maxWidth = MAX_WIDTH - x - EmiTexture.FULL_ARROW.width;
        int maxRowSize = maxWidth / EmiTexture.SLOT.width;
        int inputsSize = getInputs().size();
        int outputsSize = getOutputs().size();
        int rowSize = Math.min(inputsSize + outputsSize, maxRowSize);
        int inputsRowSize = rowSize * inputsSize / (inputsSize + outputsSize);
        int outputsRowSize = rowSize - inputsRowSize;
        int slotsColumnSize = Math.max((inputsSize + inputsRowSize - 1) / inputsRowSize, (outputsSize + outputsRowSize - 1) / outputsRowSize);
        return Math.max(textHeight + EmiTexture.SLOT.height, EmiTexture.SLOT.height * slotsColumnSize);
    }
    @Override
    public void addWidgets(WidgetHolder widgets) {
        Font textRenderer = Minecraft.getInstance().font;
        int textHeight = textRenderer.lineHeight;
        int eUtTier = RecipeHelper.getRecipeEUtTier(gtRecipe);
        String tierText = GTValues.VNF[eUtTier];
        int textWidth = textRenderer.width(tierText);
        int x = Math.max(EmiTexture.SLOT.width, textWidth);
        int maxWidth = MAX_WIDTH - x - EmiTexture.FULL_ARROW.width;
        int maxRowSize = maxWidth / EmiTexture.SLOT.width;
        int inputsSize = getInputs().size();
        int outputsSize = getOutputs().size();
        int rowSize = Math.min(inputsSize + outputsSize, maxRowSize);
        int inputsRowSize = rowSize * inputsSize / (inputsSize + outputsSize);
        int outputsRowSize = rowSize - inputsRowSize;
        int slotsColumnSize = Math.max((inputsSize + inputsRowSize - 1) / inputsRowSize, (outputsSize + outputsRowSize - 1) / outputsRowSize);
        int displayHeight = Math.max(textHeight + EmiTexture.SLOT.height, EmiTexture.SLOT.height * slotsColumnSize);
        int y = (displayHeight - EmiTexture.SLOT.height * slotsColumnSize) / 2;
        for (int i = 0; i < inputsSize; i++) {
            addSlot(widgets, getInputs().get(i), x + i % inputsRowSize * EmiTexture.SLOT.width, y + i / inputsRowSize * EmiTexture.SLOT.height);
        }
        int duration = gtRecipe.duration;
        List<Component> texts = new ArrayList<>(4);
        texts.add(EmiPort.translatable("emi.cooking.time", DecimalFormat.getInstance().format(duration / 20.0)));
        long inputEUt = RecipeHelper.getInputEUt(gtRecipe);
        if (inputEUt != 0) {
            texts.add(Component.literal("%d EU".formatted(inputEUt * duration)));
            texts.add(Component.literal("-%d EU/t".formatted(inputEUt)));
        }
        long outputEUt = RecipeHelper.getOutputEUt(gtRecipe);
        if (outputEUt != 0) {
            texts.add(Component.literal("%d EU".formatted(outputEUt * duration)));
            texts.add(Component.literal("+%d EU/t".formatted(outputEUt)));
        }
        int middleWidth = Math.max(EmiTexture.FULL_ARROW.width, texts.stream().mapToInt(textRenderer::width).max().orElseThrow());
        x += inputsRowSize * EmiTexture.SLOT.width;
        y = (displayHeight - EmiTexture.FULL_ARROW.height) / 2;
        widgets.addFillingArrow(x + (middleWidth - EmiTexture.FULL_ARROW.width) / 2, y, duration * 50);
        y = Math.max(0, (displayHeight - textHeight * texts.size()) / 2);
        for (var iterator = texts.listIterator(); iterator.hasNext(); ) {
            int i = iterator.nextIndex();
            Component text = iterator.next();
            widgets.addText(text, x, y + textHeight * i, 0xFF555555, false);
        }
        x += middleWidth;
        for (int i = 0; i < outputsSize; i++) {
            addSlot(widgets, getOutputs().get(i), x + i % outputsRowSize * EmiTexture.SLOT.width, y + i / outputsRowSize * EmiTexture.SLOT.height);
        }
        y = (displayHeight - EmiTexture.SLOT.height - textHeight) / 2;
        widgets.addSlot(EmiIngredient.of(getCatalysts()), 0, y).drawBack(false);
        widgets.addText(Component.literal(tierText), 0, y + EmiTexture.SLOT.height, -1, false);
    }
}
