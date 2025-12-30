package de.theidler.create_mobile_packages.index;

import de.theidler.create_mobile_packages.CreateMobilePackages;
import de.theidler.create_mobile_packages.recipe.RoboBeeClearRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

public class CMPRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateMobilePackages.MODID);

    public static final Supplier<RecipeSerializer<RoboBeeClearRecipe>> ROBO_BEE_CLEAR =
            RECIPE_SERIALIZERS.register("robo_bee_clear", () -> RoboBeeClearRecipe.SERIALIZER);

    @ApiStatus.Internal
    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
