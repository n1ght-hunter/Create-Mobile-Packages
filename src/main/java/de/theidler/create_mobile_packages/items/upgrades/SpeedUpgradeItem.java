package de.theidler.create_mobile_packages.items.upgrades;

import de.theidler.create_mobile_packages.index.config.CMPConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SpeedUpgradeItem extends Item {

    public SpeedUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        int multiplier = CMPConfigs.server().speedUpgradeMultiplier.get();
        tooltipComponents.add(Component.translatable("tooltip.create_mobile_packages.speed_upgrade", multiplier).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
