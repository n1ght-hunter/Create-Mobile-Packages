package de.theidler.create_mobile_packages.recipe;

import de.theidler.create_mobile_packages.index.CMPDataComponents;
import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Custom recipe that clears the frequency from a Robo Bee while preserving the return-to-sender setting.
 * This allows the output slot to show the correct tooltip.
 */
public class RoboBeeClearRecipe extends CustomRecipe {

    public static final SimpleCraftingRecipeSerializer<RoboBeeClearRecipe> SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(RoboBeeClearRecipe::new);

    public RoboBeeClearRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, @NotNull Level level) {
        ItemStack roboBee = null;
        int itemCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                itemCount++;
                if (stack.getItem() instanceof RoboBeeItem) {
                    roboBee = stack;
                }
            }
        }

        // Only match if there's exactly one item and it's a tuned Robo Bee
        return itemCount == 1 && roboBee != null && roboBee.has(CMPDataComponents.CMP_FREQ);
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.@NotNull Provider registries) {
        ItemStack roboBee = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof RoboBeeItem) {
                roboBee = stack;
                break;
            }
        }

        if (roboBee == null) {
            return ItemStack.EMPTY;
        }

        // Create output with return-to-sender preserved but frequency cleared
        ItemStack result = new ItemStack(CMPItems.ROBO_BEE.get());

        // Only set return-to-sender if it's explicitly false (non-default)
        // This ensures the output stacks with fresh bees when return-to-sender is true (default)
        Boolean returnToSender = roboBee.get(CMPDataComponents.RETURN_TO_SENDER);
        if (returnToSender != null && !returnToSender) {
            result.set(CMPDataComponents.RETURN_TO_SENDER, false);
        }

        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public @NotNull RecipeSerializer<RoboBeeClearRecipe> getSerializer() {
        return SERIALIZER;
    }
}
