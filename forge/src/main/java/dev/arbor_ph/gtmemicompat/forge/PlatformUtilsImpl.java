package dev.arbor_ph.gtmemicompat.forge;

import lombok.experimental.UtilityClass;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;

@UtilityClass
public class PlatformUtilsImpl {
    public ItemStack getRecipeRemainder(ItemStack itemStack) {
        return ForgeHooks.getCraftingRemainingItem(itemStack);
    }
}
