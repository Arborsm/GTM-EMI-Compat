package dev.arbor_ph.gtmemicompat.fabric;

import lombok.experimental.UtilityClass;
import net.minecraft.world.item.ItemStack;

@UtilityClass
public class PlatformUtilsImpl {
    public ItemStack getRecipeRemainder(ItemStack itemStack) {
        return itemStack.getRecipeRemainder();
    }
}
