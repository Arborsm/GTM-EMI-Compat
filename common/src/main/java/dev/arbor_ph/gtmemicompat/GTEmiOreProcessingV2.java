package dev.arbor_ph.gtmemicompat;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.OreProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.integration.emi.oreprocessing.GTOreProcessingEmiCategory;
import com.gregtechceu.gtceu.integration.emi.recipe.GTRecipeTypeEmiCategory;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.TankWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import static com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey.ORE;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;

@Getter
public class GTEmiOreProcessingV2 implements EmiRecipe {
    public static final EmiTexture BASE = texture("base");
    public static final EmiTexture CHEM = texture("chem");
    public static final EmiTexture SEP = texture("sep");
    public static final EmiTexture SIFT = texture("sift");
    public static final EmiTexture SMELT = texture("smelt");
    public static final int TEXTURE_WIDTH = 186;
    public static final int TEXTURE_HEIGHT = 174;
    public static final int WIDTH = 111 + 18 + 5 + 18 + 18;
    public static final int HEIGHT = 102 + 18 + 1 + 18 + 3 + 18;
    public static final NumberFormat PERCENT_FORMAT;
    public static final EmiIngredient STONE_DUSTS;
    public static final EmiIngredient CAULDRON;
    public static final EmiStack WATER = EmiStack.of(Fluids.WATER, FluidHelper.getBucket());//EmiIngredient.of(List.of(EmiStack.of(Fluids.WATER, FluidHelper.getBucket()),EmiStack.of(GTMaterials.DistilledWater.getFluid(),FluidHelper.getBucket() / 10)));
    public static final EmiStack DISTILL_WATER = EmiStack.of(GTMaterials.DistilledWater.getFluid(), FluidHelper.getBucket() / 10);
    public static final LoadingCache<Set<GTRecipeType>, List<EmiIngredient>> CATALYSTS_CACHE = CacheBuilder.newBuilder().build(CacheLoader.from(GTEmiOreProcessingV2::getCatalysts));
    static {
        //PERCENT_FORMAT
        if ((PERCENT_FORMAT = DecimalFormat.getPercentInstance()) instanceof DecimalFormat decimalFormat) {
            decimalFormat.setMaximumFractionDigits(4);
        }
        //STONE_DUSTS
        List<EmiIngredient> stoneDustStacks = new ArrayList<>();
        for (TagPrefix tagPrefix : TagPrefix.ORES.keySet()) {
            Material stoneDustMaterial = GTRegistries.MATERIALS.get(FormattingUtil.toLowerCaseUnder(tagPrefix.name));
            if (stoneDustMaterial != null) {
                stoneDustStacks.add(EmiStack.of(ChemicalHelper.get(TagPrefix.dust, stoneDustMaterial)));
            }
        }
        STONE_DUSTS = EmiIngredient.of(stoneDustStacks);
        //CAULDRON
        List<EmiIngredient> cauldronList = new ArrayList<>(getMachines(List.of(ORE_WASHER_RECIPES)));
        cauldronList.add(EmiStack.of(Items.CAULDRON));
        CAULDRON = EmiIngredient.of(cauldronList);
    }
    public static EmiTexture texture(String key) {
        return new EmiTexture(GTCEu.id("textures/gui/arrows/oreby-" + key + ".png"), 3, 3, WIDTH, HEIGHT, WIDTH, HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
    public static EmiIngredient getWorkstations(EmiRecipeCategory category) {
        return EmiIngredient.of(EmiApi.getRecipeManager().getWorkstations(category));
    }
    public static EmiIngredient getWorkstations(GTRecipeType recipeType) {
        return getWorkstations(GTRecipeTypeEmiCategory.CATEGORIES.apply(recipeType));
    }
    public static MutableComponent getTierBoostText(double boostPerTier) {
        return Component.translatable("gtceu.gui.content.tier_boost", PERCENT_FORMAT.format(boostPerTier));
    }
    public static List<EmiStack> getMachines(Iterable<GTRecipeType> validTypes) {
        List<EmiStack> list = new ArrayList<>();
        Set<MachineDefinition> set = new HashSet<>();
        for (MachineDefinition machine : GTRegistries.MACHINES) {
            if (machine.getRecipeTypes() == null) continue;
            for (GTRecipeType type : machine.getRecipeTypes()) {
                for (GTRecipeType validType : validTypes) {
                    if (type == validType && !set.contains(machine)) {
                        set.add(machine);
                        list.add(EmiStack.of(machine.asStack()));
                    }
                }
            }
        }
        return list;
    }
    public static List<EmiIngredient> getCatalysts(Set<GTRecipeType> validTypes) {
        List<EmiIngredient> catalysts = new ArrayList<>();
        catalysts.add(EmiStack.of(Items.CAULDRON));
        if (validTypes.contains(FURNACE_RECIPES)) {
            catalysts.add(EmiStack.of(Items.FURNACE));
        }
        catalysts.addAll(getMachines(validTypes));
        //because of the search mechanism of EMI, we have to pack all workstations into one EmiIngredient
        EmiIngredient catalyst = EmiIngredient.of(catalysts);
        catalysts.clear();
        catalysts.add(catalyst);
        return catalysts;
    }
    public static TankWidget addTank(WidgetHolder widgets, EmiIngredient stack, int x, int y) {
        long amount = stack.getAmount();
        long minHeight = 3;
        long capacity = 16 * amount / FluidHelper.getBucket() < minHeight ? amount * 16 / minHeight : Math.max(amount, FluidHelper.getBucket());
        return widgets.addTank(stack, x, y, EmiTexture.SLOT.width, EmiTexture.SLOT.height, (int) capacity);
    }
    @ApiStatus.Internal
    public static void register(EmiRegistry registry) {
        registry.removeRecipes(GTEmiOreProcessingV2::remove);
        for (Material mat : GTRegistries.MATERIALS) {
            if (mat.hasProperty(ORE)) {
                registry.addRecipe(new GTEmiOreProcessingV2(mat));
            }
        }
    }
    private static boolean remove(EmiRecipe r) {
        return r.getCategory() == GTOreProcessingEmiCategory.CATEGORY && (r.getId() == null || !r.getId().getPath().startsWith("/"));
    }
    private final Material material;
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs = new ArrayList<>();
    private final List<EmiStack> outputs = new ArrayList<>();
    private final List<EmiIngredient> catalysts;
    private final EmiIngredient ore;
    private final EmiStack smelt;
    private final EmiStack crushed;
    private final EmiStack crushed2;
    private final EmiStack crushedPurified;
    private final EmiStack crushedRefined;
    private final EmiStack dustImpure;
    private final EmiStack dustPure;
    private final EmiStack dust;
    private final EmiStack dustGem;
    private final EmiStack bathFluid;
    private final EmiStack byProduct0dust;
    private final EmiStack byProduct0dustTiny;
    private final EmiStack byProduct0dustTiny3;
    private final EmiStack byProduct1;
    private final EmiStack byProduct1dustTiny;
    private final EmiStack byProduct1dustTiny3;
    private final EmiStack byProduct3;
    private final EmiStack seperatedBP0;
    private final EmiStack seperatedBP1;
    private final EmiStack gemExquisite;
    private final EmiStack gemFlawless;
    private final EmiStack gem;
    private final EmiStack gemFlawed;
    private final EmiStack gemChipped;
    public GTEmiOreProcessingV2(Material material) {
        this.material = material;
        id = GTCEu.id("/ore_processing/" + material.getName());
        Set<GTRecipeType> validTypes = new HashSet<>();
        validTypes.add(MACERATOR_RECIPES);
        validTypes.add(ORE_WASHER_RECIPES);
        validTypes.add(CENTRIFUGE_RECIPES);
        validTypes.add(THERMAL_CENTRIFUGE_RECIPES);
        OreProperty oreProperty = material.getProperty(PropertyKey.ORE);
        //(raw) ore
        List<ItemStack> oreStacks = new ArrayList<>();
        for (TagPrefix tagPrefix : TagPrefix.ORES.keySet()) {
            ItemStack oreStack = ChemicalHelper.get(tagPrefix, material);
            oreStacks.add(oreStack);
        }
        ore = EmiIngredient.of(oreStacks.stream().map(EmiStack::of).toList());
        inputs.add(getOre());
        inputs.add(EmiStack.of(ChemicalHelper.get(TagPrefix.rawOre, material)));
        //crushed
        crushed = EmiStack.of(ChemicalHelper.get(TagPrefix.crushed, material));
        inputs.add(getCrushed());
        //crushed from ore
        crushed2 = getCrushed().copy().setAmount(2L * oreProperty.getOreMultiplier());
        inputs.add(getCrushed2());
        outputs.add(getCrushed2());
        //crushedPurified
        crushedPurified = EmiStack.of(ChemicalHelper.get(TagPrefix.crushedPurified, material));
        inputs.add(getCrushedPurified());
        outputs.add(getCrushedPurified());
        //crushedRefined
        crushedRefined = EmiStack.of(ChemicalHelper.get(TagPrefix.crushedRefined, material));
        inputs.add(getCrushedRefined());
        outputs.add(getCrushedRefined());
        //dustImpure
        dustImpure = EmiStack.of(ChemicalHelper.get(TagPrefix.dustImpure, material));
        inputs.add(getDustImpure());
        outputs.add(getDustImpure());
        //dustPure
        dustPure = EmiStack.of(ChemicalHelper.get(TagPrefix.dustPure, material));
        inputs.add(getDustPure());
        outputs.add(getDustPure());
        //dust
        dust = EmiStack.of(ChemicalHelper.get(TagPrefix.dust, material));
        outputs.add(getDust());
        //byProducts
        List<Material> byProducts = oreProperty.getOreByProducts();
        Material bpMaterial0 = GTUtil.selectItemInList(0, material, byProducts, Material.class);
        Material bpMaterial1 = GTUtil.selectItemInList(1, material, byProducts, Material.class);
        Material bpMaterial3 = GTUtil.selectItemInList(3, material, byProducts, Material.class);
        ItemStack crushBPStack = ChemicalHelper.get(TagPrefix.gem, bpMaterial0);
        if (crushBPStack.isEmpty()) {
            crushBPStack = ChemicalHelper.get(TagPrefix.dust, bpMaterial0);
        }
        byProduct0dust = EmiStack.of(crushBPStack).setChance(0.14f);
        byProduct0dustTiny = EmiStack.of(ChemicalHelper.get(TagPrefix.dustTiny, bpMaterial0));
        outputs.add(getByProduct0dustTiny());
        outputs.add(getByProduct0dust());
        byProduct0dustTiny3 = getByProduct0dustTiny().copy().setAmount(3);
        outputs.add(getByProduct0dustTiny3());
        byProduct1 = EmiStack.of(ChemicalHelper.get(TagPrefix.dust, bpMaterial1)).setChance(0.14f);
        outputs.add(getByProduct1());
        byProduct1dustTiny = EmiStack.of(ChemicalHelper.get(TagPrefix.dustTiny, bpMaterial1));
        outputs.add(getByProduct1dustTiny());
        byProduct1dustTiny3 = getByProduct1dustTiny().copy().setAmount(3);
        outputs.add(getByProduct1dustTiny3());
        //washer second input
        inputs.add(WATER);
        inputs.add(DISTILL_WATER);
        //smelt
        Material smeltMaterial = Objects.requireNonNullElse(oreProperty.getDirectSmeltResult(), material);
        TagPrefix smeltTagPrefix;
        if (smeltMaterial.hasProperty(PropertyKey.INGOT)) {
            smeltTagPrefix = TagPrefix.ingot;
        } else if (smeltMaterial.hasProperty(PropertyKey.GEM)) {
            smeltTagPrefix = TagPrefix.gem;
        } else if (smeltMaterial.hasProperty(PropertyKey.DUST)) {
            smeltTagPrefix = TagPrefix.dust;
        } else {
            smeltTagPrefix = null;
        }
        if (smeltTagPrefix == null) {
            smelt = EmiStack.EMPTY;
        } else {
            smelt = EmiStack.of(ChemicalHelper.get(smeltTagPrefix, smeltMaterial));
            outputs.add(getSmelt());
            if (!material.hasProperty(PropertyKey.BLAST)) {
                validTypes.add(FURNACE_RECIPES);
                //catalysts.add(EmiStack.of(Items.FURNACE));
            }
        }
        //bath BP
        Pair<Material, Integer> washedIn = oreProperty.getWashedIn();
        if (washedIn.getLeft() != null) {
            bathFluid = EmiStack.of(washedIn.getLeft().getFluid(), washedIn.getRight() * FluidHelper.getBucket() / 1000);
            inputs.add(getBathFluid());
            byProduct3 = EmiStack.of(ChemicalHelper.get(TagPrefix.dust, bpMaterial3)).copy().setChance(0.7f);
            outputs.add(getByProduct3());
            validTypes.add(CHEMICAL_BATH_RECIPES);
        } else {
            byProduct3 = bathFluid = EmiStack.EMPTY;
        }
        //Separatied BP
        List<Material> separatedInto = oreProperty.getSeparatedInto();
        if (separatedInto != null && !separatedInto.isEmpty()) {
            seperatedBP0 = EmiStack.of(ChemicalHelper.get(TagPrefix.dustSmall, separatedInto.get(0))).setChance(0.4f);
            outputs.add(getSeperatedBP0());
            Material separatedInto1 = separatedInto.get(separatedInto.size() - 1);
            boolean isNugget = separatedInto1.getBlastTemperature() == 0 && separatedInto1.hasProperty(PropertyKey.INGOT);
            seperatedBP1 = EmiStack.of(ChemicalHelper.get(isNugget ? TagPrefix.nugget : TagPrefix.dustSmall, separatedInto1), isNugget ? 2 : 1).setChance(0.2f);
            outputs.add(getSeperatedBP1());
            validTypes.add(ELECTROMAGNETIC_SEPARATOR_RECIPES);
        } else {
            seperatedBP0 = seperatedBP1 = EmiStack.EMPTY;
        }
        //sifter
        if (material.hasProperty(PropertyKey.GEM)) {
            gemExquisite = EmiStack.of(ChemicalHelper.get(TagPrefix.gemExquisite, material)).setChance(0.03f);
            outputs.add(getGemExquisite());
            gemFlawless = EmiStack.of(ChemicalHelper.get(TagPrefix.gemFlawless, material)).setChance(0.1f);
            outputs.add(getGemFlawless());
            gem = EmiStack.of(ChemicalHelper.get(TagPrefix.gem, material)).setChance(0.5f);
            dustGem = getDust().copy().setChance(0.25f);
            gemFlawed = EmiStack.of(ChemicalHelper.get(TagPrefix.gemFlawed, material)).setChance(0.25f);
            outputs.add(getGemFlawed());
            gemChipped = EmiStack.of(ChemicalHelper.get(TagPrefix.gemChipped, material)).setChance(0.35f);
            outputs.add(getGemChipped());
            validTypes.add(SIFTER_RECIPES);
        } else {
            gemExquisite = gemFlawless = gem = dustGem = gemFlawed = gemChipped = EmiStack.EMPTY;
        }
        catalysts = CATALYSTS_CACHE.getUnchecked(validTypes);
        //catalysts.add(EmiStack.of(Items.CAULDRON));
        //catalysts.addAll(MACHINES_CACHE.getUnchecked(validTypes));
        ////because of the search mechanism of EMI, we have to pack all workstations into one EmiIngredient
        //EmiIngredient catalyst = EmiIngredient.of(catalysts);
        //catalysts.clear();
        //catalysts.add(catalyst);
    }
    @Override
    public EmiRecipeCategory getCategory() {
        return GTOreProcessingEmiCategory.CATEGORY;
    }
    @Override
    public int getDisplayWidth() {
        return WIDTH;
    }
    @Override
    public int getDisplayHeight() {
        return HEIGHT;
    }
    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(BASE, 0, 0);
        MutableComponent boost085 = getTierBoostText(0.085);
        widgets.addSlot(STONE_DUSTS, 18 + 25 + 18 + 9, 0).recipeContext(this);
        //ore --macerator-> crushed
        {
            widgets.addSlot(getOre(), 0, 0);
            widgets.addSlot(getWorkstations(MACERATOR_RECIPES), 0, 18 + (26 - 18) / 2).drawBack(false);
            widgets.addSlot(getCrushed2(), 0, 18 + 26).recipeContext(this);
            widgets.addSlot(getByProduct0dust(), 0, 18 * 2 + 26).recipeContext(this).appendTooltip(boost085);
            Set<EmiRecipe> recipes = new HashSet<>();
            for (EmiStack stack : getOre().getEmiStacks()) {
                recipes.addAll(EmiApi.getRecipeManager().getRecipesByInput(stack));
            }
            GTRecipeTypeEmiCategory category = GTRecipeTypeEmiCategory.CATEGORIES.apply(MACERATOR_RECIPES);
            recipes.retainAll(new HashSet<>(EmiApi.getRecipeManager().getRecipes(category)));
            Set<EmiIngredient> stacks = new HashSet<>();
            stacks.add(getOre());
            for (EmiRecipe recipe : recipes) {
                stacks.addAll(recipe.getOutputs());
            }
            int height = 18 + 26 + 18 * 2;
            widgets.add(new SetPagesWidgets(new Bounds(0, 0, 18, height), Map.of(category, new ArrayList<>(recipes)), stacks, -1, -1, List.of(height + 1, 18 + 1, -height - 1, -18 - 1)));
        }
        //crushed --washer-> crushedPurified
        {
            int y = 22;
            widgets.addSlot(getWorkstations(ORE_WASHER_RECIPES), 21, y).drawBack(false);
            addTank(widgets, WATER, 39, y);
            widgets.addSlot(getCrushedPurified(), 61, y).recipeContext(this);
            widgets.addSlot(getByProduct0dustTiny3(), 61 + 18, y).recipeContext(this);
            Set<EmiRecipe> recipes = new HashSet<>(EmiApi.getRecipeManager().getRecipesByInput(getCrushed()));
            GTRecipeTypeEmiCategory category = GTRecipeTypeEmiCategory.CATEGORIES.apply(ORE_WASHER_RECIPES);
            recipes.retainAll(new HashSet<>(EmiApi.getRecipeManager().getRecipes(category)));
            recipes.retainAll(new HashSet<>(EmiApi.getRecipeManager().getRecipesByOutput(getByProduct0dustTiny3())));
            Set<EmiIngredient> stacks = new HashSet<>();
            stacks.add(getCrushed());
            stacks.add(WATER);
            stacks.add(DISTILL_WATER);
            for (EmiRecipe recipe : recipes) {
                stacks.addAll(recipe.getOutputs());
            }
            int width = 61 + 18;
            widgets.add(new SetPagesWidgets(new Bounds(18, y, width, 18), Map.of(category, new ArrayList<>(recipes)), stacks, -1, 18 + 26 - 1, List.of(18 + 1, 18 + 5, -18 - 4, width - 4, -18 - 1, -width, 18 + 4, -18 - 1)));
        }
        //crushed --macerator-> dustImpure
        {
            int x = 20;
            widgets.addSlot(getWorkstations(MACERATOR_RECIPES), x, 67).drawBack(false);
            widgets.addSlot(getDustImpure(), x, 89).recipeContext(this);
            widgets.addSlot(getByProduct0dust(), x, 89 + 18).recipeContext(this).appendTooltip(boost085);
            Set<EmiRecipe> recipes = new HashSet<>(EmiApi.getRecipeManager().getRecipesByInput(getCrushed()));
            GTRecipeTypeEmiCategory category = GTRecipeTypeEmiCategory.CATEGORIES.apply(MACERATOR_RECIPES);
            recipes.retainAll(new HashSet<>(EmiApi.getRecipeManager().getRecipes(category)));
            Set<EmiIngredient> stacks = new HashSet<>();
            stacks.add(getCrushed());
            for (EmiRecipe recipe : recipes) {
                stacks.addAll(recipe.getOutputs());
            }
            int height = 25 + 18 * 2;
            widgets.add(new SetPagesWidgets(new Bounds(x, 67 - 3, 18, height), Map.of(category, new ArrayList<>(recipes)), stacks, -1, 18 + 26 - 1, List.of(18 + 1, 18 + 1, 5, 1, height - 3, 18, -height - 1, -18 + 3, -11, -4, -9, -18 - 1)));
        }
        //dustImpure --centrifuge-> dust
        {
            int x = 47;
            widgets.addSlot(getWorkstations(CENTRIFUGE_RECIPES), x, 77).drawBack(false);
            widgets.addSlot(getDust(), x, 77 + 18 + 3).recipeContext(this);
            widgets.addSlot(getByProduct0dustTiny(), x, 77 + 18 + 3 + 18).recipeContext(this);
            Set<EmiRecipe> recipes = new HashSet<>(EmiApi.getRecipeManager().getRecipesByInput(getDustImpure()));
            GTRecipeTypeEmiCategory category = GTRecipeTypeEmiCategory.CATEGORIES.apply(CENTRIFUGE_RECIPES);
            recipes.retainAll(new HashSet<>(EmiApi.getRecipeManager().getRecipes(category)));
            Set<EmiIngredient> stacks = new HashSet<>();
            stacks.add(getDustImpure());
            for (EmiRecipe recipe : recipes) {
                stacks.addAll(recipe.getOutputs());
            }
            int height = 25 + 18 * 2;
            widgets.add(new SetPagesWidgets(new Bounds(x-4, 77-4, 18+4, height), Map.of(category, new ArrayList<>(recipes)), stacks, 20-1, 89-1, List.of(18 + 1, 18 + 5, 9 + 18, 4 + 18 + 1,-height-1,-18-9,16,-18-1)));
        }
        //crushedPurified --macerator-> dustPure
        widgets.addSlot(getWorkstations(MACERATOR_RECIPES), 111, 44).drawBack(false);
        widgets.addSlot(getDustPure(), 111 + 18 + 5, 44).recipeContext(this);
        widgets.addSlot(getByProduct1(), 111 + 18 + 5 + 18, 44).recipeContext(this).appendTooltip(boost085);
        //dustPure --centrifuge-> dust
        widgets.addSlot(getWorkstations(CENTRIFUGE_RECIPES), 131, 66).drawBack(false);
        widgets.addSlot(getDust(), 131, 66 + 18 + 4).recipeContext(this);
        widgets.addSlot(getByProduct1dustTiny(), 131, 66 + 18 + 4 + 18).recipeContext(this);
        //crushedPurified --thermal_centrifuge-> crushedRefined
        widgets.addSlot(getWorkstations(THERMAL_CENTRIFUGE_RECIPES), 94, 67).drawBack(false);
        widgets.addSlot(getCrushedRefined(), 94, 67 + 18 + 4).recipeContext(this);
        widgets.addSlot(getByProduct1dustTiny3(), 94, 67 + 18 + 4 + 18).recipeContext(this);
        //crushedRefined --macerator-> dust
        widgets.addSlot(getWorkstations(MACERATOR_RECIPES), 68, 77).drawBack(false);
        widgets.addSlot(getDust(), 68, 77 + 18 + 3).recipeContext(this);
        widgets.addSlot(getByProduct1(), 68, 77 + 18 + 3 + 18).recipeContext(this).appendTooltip(boost085);
        //crushed --cauldron-> crushedPurified
        widgets.addSlot(getCrushed2().copy().setAmount(1), 0, 102);
        widgets.addSlot(CAULDRON, 0, 102 + 1 + 18).drawBack(false);
        int yLastLine = 102 + 18 + 1 + 18 + 3;
        widgets.addSlot(getCrushedPurified(), 0, yLastLine).recipeContext(this);
        //dustImpure --cauldron-> dust
        widgets.addSlot(getDustImpure(), 18 + 2, yLastLine);
        widgets.addSlot(CAULDRON, 18 + 2 + 18 + 1, yLastLine).drawBack(false);
        widgets.addSlot(getDust(), 18 + 2 + 18 + 1 + 18 + 3, yLastLine).recipeContext(this);
        //dustPure --cauldron-> dust
        widgets.addSlot(getDustPure(), 81, yLastLine);
        widgets.addSlot(CAULDRON, 81 + 18 + 1, yLastLine).drawBack(false);
        widgets.addSlot(getDust(), 81 + 18 + 1 + 18 + 3, yLastLine).recipeContext(this);
        //ore --furnace-> smelt
        if (!getSmelt().isEmpty() && !getMaterial().hasProperty(PropertyKey.BLAST)) {
            widgets.addTexture(SMELT, 0, 0);//widgets.addTexture(SMELT_ARROW, SLOT.width, (SLOT.height - SMELT_ARROW.height) / 2);
            EmiRecipeCategory category =/* material.hasProperty(PropertyKey.BLAST) ? GTRecipeTypeEmiCategory.CATEGORIES.apply(GTRecipeTypes.BLAST_RECIPES) :*/ VanillaEmiRecipeCategories.SMELTING;
            widgets.addSlot(getWorkstations(category), 18 + (25 - 18) / 2, 0).drawBack(false);
            widgets.addSlot(getSmelt(), 18 + 25, 0).recipeContext(this);
        }
        //crushed --bath-> crushedPurified
        if (!getBathFluid().isEmpty()) {
            widgets.addTexture(CHEM, 0, 0);
            widgets.addSlot(getWorkstations(CHEMICAL_BATH_RECIPES), 21, 45).drawBack(false);
            addTank(widgets, getBathFluid(), 39, 45);
            widgets.addSlot(getCrushedPurified(), 61, 45).recipeContext(this);
            widgets.addSlot(getByProduct3(), 79, 45).recipeContext(this).appendTooltip(getTierBoostText(0.058));
        }
        //dustPure --electromagnetic_separator-> dust
        if (!getSeperatedBP0().isEmpty()) {
            widgets.addTexture(SEP, 0, 0);
            int xSep = 131 + 18 + 3;
            widgets.addSlot(getWorkstations(ELECTROMAGNETIC_SEPARATOR_RECIPES), xSep, 66).drawBack(false);
            widgets.addSlot(getDust(), xSep, 66 + 18 + 4).recipeContext(this);
            widgets.addSlot(getSeperatedBP0(), xSep, 66 + 18 + 4 + 18).recipeContext(this);
            widgets.addSlot(getSeperatedBP1(), xSep, 66 + 18 + 4 + 18 * 2).recipeContext(this);
        }
        //crushedPurified --sifter-> gem
        if (!getGem().isEmpty()) {
            widgets.addTexture(SIFT, 0, 0);
            widgets.addSlot(getWorkstations(SIFTER_RECIPES), 98, 20).drawBack(false);
            int xGem = 98 + 18;
            widgets.addSlot(getGemExquisite(), xGem, 0).recipeContext(this).appendTooltip(getTierBoostText(0.01));
            widgets.addSlot(getGemFlawless(), xGem + 18, 0).recipeContext(this).appendTooltip(getTierBoostText(0.015f));
            widgets.addSlot(getGem(), xGem + 18 * 2, 0).recipeContext(this).appendTooltip(getTierBoostText(0.075f));
            widgets.addSlot(getDustGem(), xGem, 18).recipeContext(this).appendTooltip(getTierBoostText(0.05f));
            widgets.addSlot(getGemFlawed(), xGem + 18, 18).recipeContext(this).appendTooltip(getTierBoostText(0.03f));
            widgets.addSlot(getGemChipped(), xGem + 18 * 2, 18).recipeContext(this).appendTooltip(getTierBoostText(0.04f));
        }
    }
    @Override
    public boolean hideCraftable() {
        return true;
    }
    @Override
    public boolean supportsRecipeTree() {
        return false;
    }
}
