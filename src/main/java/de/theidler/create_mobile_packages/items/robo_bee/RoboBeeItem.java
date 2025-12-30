package de.theidler.create_mobile_packages.items.robo_bee;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.CMPHelper;
import de.theidler.create_mobile_packages.blocks.advanced_bee_port.AdvancedBeePortBlockEntity;
import de.theidler.create_mobile_packages.index.CMPDataComponents;
import de.theidler.create_mobile_packages.index.config.CMPConfigs;
import de.theidler.create_mobile_packages.items.portable_stock_ticker.StockCheckingItem;
import de.theidler.create_mobile_packages.robo.PlayerTarget;
import de.theidler.create_mobile_packages.robo.RoboManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class RoboBeeItem extends StockCheckingItem {

    public RoboBeeItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (super.useOn(context) != InteractionResult.PASS) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack offhandItem = player.getOffhandItem();

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        // Require a package in the offhand to spawn the bee
        if (!PackageItem.isPackage(offhandItem)) {
            player.displayClientMessage(
                    Component.translatable("message.create_mobile_packages.robo_bee.no_package").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.FAIL;
        }

        // Check if package transport via spawn is allowed
        if (!CMPConfigs.server().allowRoboBeeSpawnPackageTransport.get()) {
            return InteractionResult.PASS;
        }

        // Validate target is reachable and check if cross-dimensional travel is needed
        boolean needsCrossDimensional = false;
        int targetSpeed = CMPConfigs.server().beeSpeed.get();
        if (level instanceof ServerLevel serverLevel) {
            String address = PackageItem.getAddress(offhandItem);
            TargetReachability reachability = checkTargetReachability(serverLevel, address, pos);
            if (!reachability.canReach()) {
                player.displayClientMessage(
                        Component.translatable("message.create_mobile_packages.robo_bee.target_unreachable").withStyle(ChatFormatting.RED),
                        true);
                return InteractionResult.FAIL;
            }
            needsCrossDimensional = reachability.requiresCrossDimensional();
            targetSpeed = reachability.targetSpeed();
        }

        ItemStack packageItem = offhandItem.copy();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        if (level instanceof ServerLevel serverLevel) {
            ItemStack beeStack = context.getItemInHand();
            UUID networkId = networkFromStack(beeStack);
            UUID finalNetworkId = networkId != null ? networkId : UUID.randomUUID();
            // Use player name as return address if return-to-sender is enabled on the bee
            boolean returnToSender = isReturnToSender(beeStack);
            String returnAddress = returnToSender ? player.getName().getString() : null;
            // Spawn bee with cross-dimensional capability and target's speed if applicable
            RoboManager.get(serverLevel).newAdvancedRobo(serverLevel, packageItem, pos, finalNetworkId, 1, targetSpeed, needsCrossDimensional, returnToSender, returnAddress, networkId);
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    /**
     * Result of target reachability check.
     * @param canReach Whether the target can be reached
     * @param requiresCrossDimensional Whether cross-dimensional travel is needed
     * @param targetSpeed The speed to use (from target Advanced Port if applicable, otherwise default)
     */
    private record TargetReachability(boolean canReach, boolean requiresCrossDimensional, int targetSpeed) {}

    /**
     * Checks if a bee can reach its target and whether it needs cross-dimensional travel.
     * Player-spawned bees can:
     * - Always reach a player (any dimension - enables cross-dimensional if needed)
     * - Reach a port in the same dimension
     * - Reach a port in another dimension ONLY if it's an Advanced Bee Port with Ender upgrade
     */
    private static TargetReachability checkTargetReachability(ServerLevel level, String address, BlockPos spawnPos) {
        int defaultSpeed = CMPConfigs.server().beeSpeed.get();

        if (address == null || address.isBlank()) {
            return new TargetReachability(false, false, defaultSpeed);
        }

        // Check if target is a player in the same dimension first
        PlayerTarget samePlayerTarget = PlayerTarget.fromAddress(level, address);
        if (samePlayerTarget.isValid()) {
            return new TargetReachability(true, false, defaultSpeed);
        }

        // Check if target is a player in another dimension
        PlayerTarget crossPlayerTarget = PlayerTarget.fromAddressAcrossDimensions(level, address);
        if (crossPlayerTarget.isValid()) {
            return new TargetReachability(true, true, defaultSpeed); // Requires cross-dimensional
        }

        // Check if target is a port in current dimension (any port type works)
        BlockEntity port = CMPHelper.getClosestAnyPort(level, address, spawnPos, null, UUID.randomUUID());
        if (port != null) {
            // If target is an Advanced Port, use its effective speed
            if (port instanceof AdvancedBeePortBlockEntity advancedPort) {
                return new TargetReachability(true, false, advancedPort.getEffectiveSpeed());
            }
            return new TargetReachability(true, false, defaultSpeed);
        }

        // Search other dimensions - only Advanced Bee Ports with Ender upgrade can be reached
        if (level.getServer() != null) {
            for (ServerLevel otherLevel : level.getServer().getAllLevels()) {
                if (otherLevel.dimension().equals(level.dimension())) continue;

                // Only check for Advanced Bee Ports with Ender upgrade in other dimensions
                AdvancedBeePortBlockEntity advancedPort = CMPHelper.getClosestAdvancedBeePort(otherLevel, address, spawnPos, null, UUID.randomUUID());
                if (advancedPort != null && advancedPort.hasEnderUpgrade()) {
                    // Use the target port's effective speed
                    return new TargetReachability(true, true, advancedPort.getEffectiveSpeed());
                }
            }
        }

        // No valid reachable target found
        return new TargetReachability(false, false, defaultSpeed);
    }

    /**
     * Called when the player scrolls while holding the item and crouching.
     * Toggles the return-to-sender mode.
     */
    public static void onScrollToggle(Player player, ItemStack stack) {
        boolean currentValue = isReturnToSender(stack);
        boolean newValue = !currentValue;
        setReturnToSender(stack, newValue);

        // Show feedback message
        Component message = newValue
                ? Component.translatable("message.create_mobile_packages.robo_bee.return_to_sender_enabled").withStyle(ChatFormatting.GREEN)
                : Component.translatable("message.create_mobile_packages.robo_bee.return_to_sender_disabled").withStyle(ChatFormatting.RED);
        player.displayClientMessage(message, true);
    }

    public static boolean isReturnToSender(ItemStack stack) {
        Boolean value = stack.get(CMPDataComponents.RETURN_TO_SENDER);
        // Default to true if not set
        return value == null || value;
    }

    public static void setReturnToSender(ItemStack stack, boolean returnToSender) {
        if (returnToSender) {
            // Remove component to use default (true)
            stack.remove(CMPDataComponents.RETURN_TO_SENDER);
        } else {
            stack.set(CMPDataComponents.RETURN_TO_SENDER, false);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.create_mobile_packages.robo_bee.robo_bee").withStyle(ChatFormatting.GRAY));
        if (CMPConfigs.server().allowRoboBeeSpawnPackageTransport.get()) {
            tooltipComponents.add(Component.translatable("tooltip.create_mobile_packages.robo_bee.package_transport").withStyle(ChatFormatting.GRAY));
        }

        // Show return to sender status
        if (isReturnToSender(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.create_mobile_packages.robo_bee.return_to_sender_enabled").withStyle(ChatFormatting.GREEN));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.create_mobile_packages.robo_bee.return_to_sender_disabled").withStyle(ChatFormatting.RED));
        }
        tooltipComponents.add(Component.translatable("tooltip.create_mobile_packages.robo_bee.return_to_sender_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}