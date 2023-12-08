package dev.arbor_ph.gtmemicompat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import lombok.experimental.UtilityClass;
import net.minecraft.world.item.ItemStack;

@UtilityClass
public class PlatformUtils {
    @ExpectPlatform
    public ItemStack getRecipeRemainder(ItemStack itemStack) {
        throw new IllegalStateException();
    }
}
