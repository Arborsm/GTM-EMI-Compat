package dev.arbor_ph.gtmemicompat;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.emi.recipe.GTRecipeTypeEmiCategory;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import dev.arbor_ph.gtmemicompat.mixin.ARecipeScreen;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import it.unimi.dsi.fastutil.longs.LongIntPair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

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
    public final GTRecipe gtRecipe;
    protected final List<EmiIngredient> inputs = new ArrayList<>();
    protected final List<EmiStack> outputs = new ArrayList<>();
    protected final List<EmiIngredient> catalysts = new ArrayList<>();
    private int tier;
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
        setTierToMin();
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
        return MAX_WIDTH;
    }
    @Override
    public int getDisplayHeight() {
        WidgetGroup widgets = new WidgetGroup(this, 0, 0, MAX_WIDTH, MAX_WIDTH + 1);
        addWidgets(widgets);
        int displayHeight = 0;
        for (Widget widget : widgets.widgets) {
            Bounds bounds = widget.getBounds();
            displayHeight = Math.max(displayHeight, bounds.y() + bounds.height());
        }
        return displayHeight;
    }
    @Override
    public void addWidgets(WidgetHolder widgets) {
        Font textRenderer = Minecraft.getInstance().font;
        int textHeight = textRenderer.lineHeight;
        int eUtTier = getTier();
        String tierText = GTValues.VNF[eUtTier];
        int tierTextWidth = textRenderer.width(tierText);
        int maxWidth = MAX_WIDTH - EmiTexture.FULL_ARROW.width;
        int maxRowLen = maxWidth / EmiTexture.SLOT.width;
        int inputsSize = getInputs().size();
        int outputsSize = getOutputs().size();
        int slotsRowLen = Math.min(inputsSize + outputsSize, maxRowLen);
        int inputsRowLen = slotsRowLen * inputsSize / (inputsSize + outputsSize);
        int outputsRowLen = slotsRowLen - inputsRowLen;
        int slotsColumnLen = Math.max(Math.floorDiv(inputsSize, inputsRowLen), Math.floorDiv(outputsSize, outputsRowLen));
        int displayHeight = Math.max(textHeight + EmiTexture.SLOT.height, EmiTexture.SLOT.height * slotsColumnLen);
        int slotsY = 0;
        int duration = gtRecipe.duration;
        long inputEUt = RecipeHelper.getInputEUt(gtRecipe);
        long outputEUt = RecipeHelper.getOutputEUt(gtRecipe);
        if (getTier() > getMinTier() && inputEUt != 0) {
            LongIntPair pair = OverclockingLogic.NON_PERFECT_OVERCLOCK.getLogic().runOverclockingLogic(gtRecipe, inputEUt, GTValues.V[getTier()], duration, GTValues.MAX);
            duration = pair.rightInt();
            inputEUt = pair.firstLong();
            tierText = tierText.formatted(ChatFormatting.ITALIC);
        }
        List<Component> texts = new ArrayList<>(4);
        texts.add(EmiPort.translatable("emi.cooking.time", DecimalFormat.getInstance().format(duration / 20.0)));
        if (inputEUt != 0) {
            texts.add(Component.literal("%d EU".formatted(inputEUt * duration)));
            texts.add(Component.literal("-%d EU/t".formatted(inputEUt)));
        }
        if (outputEUt != 0) {
            texts.add(Component.literal("%d EU".formatted(outputEUt * duration)));
            texts.add(Component.literal("+%d EU/t".formatted(outputEUt)));
        }
        int middleWidth = Math.max(EmiTexture.FULL_ARROW.width, texts.stream().mapToInt(textRenderer::width).max().orElseThrow());
        int displayWidth = middleWidth + slotsRowLen * EmiTexture.SLOT.width;
        int infoX = (MAX_WIDTH - displayWidth) / 2;
        int x = infoX;
        for (int i = 0; i < inputsSize; i++) {
            addSlot(widgets, getInputs().get(i), x + i % inputsRowLen * EmiTexture.SLOT.width, slotsY + i / inputsRowLen * EmiTexture.SLOT.height);
        }
        x += inputsRowLen * EmiTexture.SLOT.width;
        widgets.addFillingArrow(x + (middleWidth - EmiTexture.FULL_ARROW.width) / 2, (displayHeight - EmiTexture.FULL_ARROW.height) / 2, duration * 50);
        int textsY = Math.max(0, (displayHeight - textHeight * texts.size()) / 2);
        for (var iterator = texts.listIterator(); iterator.hasNext(); ) {
            int i = iterator.nextIndex();
            Component text = iterator.next();
            widgets.addText(text, x, textsY + textHeight * i, 0xFF555555, false);
        }
        x += middleWidth;
        for (int i = 0; i < outputsSize; i++) {
            addSlot(widgets, getOutputs().get(i), x + i % outputsRowLen * EmiTexture.SLOT.width, slotsY + i / outputsRowLen * EmiTexture.SLOT.height).recipeContext(this);
        }
        int infoY = slotsY + EmiTexture.SLOT.height * slotsColumnLen;
        widgets.addSlot(EmiIngredient.of(getCatalysts()), infoX, infoY).drawBack(false);
        int tierY = infoY + EmiTexture.SLOT.height;
        widgets.addButton(infoX, tierY, EmiTexture.SLOT.width, textHeight, 0, 12 * 3, () -> true, (mouseX, mouseY, button) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                setTier(getTier() + 1);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                setTier(getTier() - 1);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                setTierToMin();
            }
            if (Minecraft.getInstance().screen instanceof RecipeScreen screen0) {
                var screen = (RecipeScreen & ARecipeScreen) screen0;
                screen.setPage(screen.getTabPage(), screen.getTab(), screen.getPage());
            }
        });
        widgets.addTooltipText(List.of(
          Component.literal("最低：" + GTValues.VNF[getMinTier()]),
          Component.literal("左键以增加超频等级"),
          Component.literal("右键以降低超频等级"),
          Component.literal("中键以重置超频等级")
        ), infoX, tierY, EmiTexture.SLOT.width, textHeight);
        widgets.addText(Component.literal(tierText), infoX + (EmiTexture.SLOT.width - tierTextWidth) / 2, tierY, -1, false);
    }
    public int getMinTier() {
        return RecipeHelper.getRecipeEUtTier(gtRecipe);
    }
    public int getTier() {
        return tier;
    }
    public void setTier(int tier) {
        this.tier = Mth.clamp(tier, getMinTier(), GTValues.MAX);
    }
    public void setTierToMin() {
        setTier(-1);
    }
}
