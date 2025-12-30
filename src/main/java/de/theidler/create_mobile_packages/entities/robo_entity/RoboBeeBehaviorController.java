package de.theidler.create_mobile_packages.entities.robo_entity;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.blocks.advanced_bee_port.AdvancedBeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.RoboRequest;
import de.theidler.create_mobile_packages.robo.PlayerTarget;
import de.theidler.create_mobile_packages.robo.RoboManager;
import de.theidler.create_mobile_packages.robo.VirtualRobo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static de.theidler.create_mobile_packages.CMPHelper.calcETA;

public class RoboBeeBehaviorController {
    private RoboBeeState state = RoboBeeState.IDLE;
    private boolean init = true;

    public void tick(VirtualRobo robo) {
        switch (state) {
            case IDLE:
                handleIdle(robo);
                break;
            case TAKEOFF:
                handleTakeoff(robo);
                break;
            case NAVIGATE_TO_TARGET:
                handleNavigateToTarget(robo);
                break;
            case DIMENSIONAL_TELEPORT:
                handleDimensionalTeleport(robo);
                break;
            case ALIGN_FOR_DELIVERY:
                handleAlignForDelivery(robo);
                break;
            case LAND:
                handleLand(robo);
                break;
            case DELIVER_PACKAGE:
                handleDeliverPackage(robo);
                break;
            case SHUTDOWN:
                handleShutdown(robo);
                break;
        }
    }

    private void handleIdle(VirtualRobo robo) {
        robo.setTargetVelocity(Vec3.ZERO);
        if (robo.getTarget() != null && robo.getTarget().isValid()) {
            setState(RoboBeeState.TAKEOFF);
        }
    }

    private void handleTakeoff(VirtualRobo robo) {
        BlockEntity startPort = robo.getStartPortBlockEntity();
        if (init) {
            openPort(startPort, true);
            init = false;
        }
        if (startPort == null) {
            setState(RoboBeeState.NAVIGATE_TO_TARGET);
            return;
        }
        Vec3 mid = getAbove(startPort, 1.6);
        Vec3 end = getAbove(startPort, 2);

        double y = robo.getCurrentPos().y;
        double speed = (robo.getSpeed() / 20.0) / 2; // Takeoff slower
        if (y < mid.y - speed) {
            moveAndScale(robo, mid, speed, 0, 1); // 1st part with scaling package
        } else if (y < end.y - speed) {
            moveTo(robo, end, speed); // 2nd part without scaling package
            robo.setPackageHeightScale(1.0f);
        } else {
            robo.setPos(end);
            robo.setTargetVelocity(Vec3.ZERO);
            openPort(startPort, false);
            setState(RoboBeeState.NAVIGATE_TO_TARGET);
        }
    }

    private void handleNavigateToTarget(VirtualRobo robo) {
        if (robo.getTargetPosition() == null) {
            setState(RoboBeeState.IDLE);
            return;
        }

        // Check if we need to teleport to another dimension
        if (robo.needsDimensionalTeleport()) {
            // Fly up to sky height first, then teleport
            double teleportHeight = 100; // Fly up before teleporting
            Vec3 teleportTarget = new Vec3(robo.getCurrentPos().x, teleportHeight, robo.getCurrentPos().z);
            double speed = robo.getSpeed() / 20.0;
            moveTo(robo, teleportTarget, speed);
            if (robo.getCurrentPos().y >= teleportHeight - speed) {
                setState(RoboBeeState.DIMENSIONAL_TELEPORT);
            }
            return;
        }

        if (robo.getTarget() != null) {
            robo.getTarget().setETA(calcETA(robo.getTargetPosition(), robo.getCurrentPos(), robo.getSpeed()));
            if (robo.getTarget() instanceof PlayerTarget playerTarget)
                playerTarget.updateEtaToast(robo);
        }
        Vec3 target = getAbove(robo.getTargetPosition(), 2);
        double speed = robo.getSpeed() / 20.0;
        moveTo(robo, target, speed);
        if (isAtTarget(robo, target, speed)) {
            if (robo.getTarget() != null) {
                robo.getTarget().setETA(0); // set ETA to 0 as the bee arrived
            }
            setState(RoboBeeState.ALIGN_FOR_DELIVERY);
            robo.setTargetVelocity(Vec3.ZERO);
        }
    }

    /**
     * Handles the dimensional teleport state.
     * Teleports the robo to the target dimension and transfers it to that dimension's RoboManager.
     */
    private void handleDimensionalTeleport(VirtualRobo robo) {
        if (!robo.canTravelDimensions()) {
            setState(RoboBeeState.NAVIGATE_TO_TARGET);
            return;
        }

        ResourceKey<Level> targetDim = robo.getTargetDimension();
        if (targetDim == null || targetDim.equals(robo.getServerLevel().dimension())) {
            // No dimension change needed
            setState(RoboBeeState.NAVIGATE_TO_TARGET);
            return;
        }

        // Despawn visual entity in current dimension
        robo.despawnEntity();

        // Get target dimension level
        ServerLevel currentLevel = robo.getServerLevel();
        MinecraftServer server = currentLevel.getServer();
        ServerLevel targetLevel = server.getLevel(targetDim);

        if (targetLevel == null) {
            // Target dimension not available, go back to navigation
            setState(RoboBeeState.NAVIGATE_TO_TARGET);
            return;
        }

        // Remove from current dimension's manager
        RoboManager.get(currentLevel).remove(robo.getId());

        // Update robo's level reference
        robo.setServerLevel(targetLevel);

        // Teleport to high above target position in the new dimension so it can fly down
        Vec3 targetPos = robo.getTargetPosition();
        double teleportHeight = 100; // Spawn high above target to fly down
        if (targetPos != null) {
            robo.setPos(new Vec3(targetPos.x, targetPos.y + teleportHeight, targetPos.z));
        }

        // Clear target dimension since we've arrived
        robo.setTargetDimension(null);

        // Add to new dimension's manager
        RoboManager.get(targetLevel).add(robo);

        // Continue to navigate to target (fly down from teleport height)
        setState(RoboBeeState.NAVIGATE_TO_TARGET);
    }

    private void handleAlignForDelivery(VirtualRobo robo) {
        if (init) {
            openPort(robo.getTarget() != null ? robo.getTarget().asPortBlockEntity() : null, true);
            init = false;
        }
        if (robo.rotateToSnap() == 0) {
            setState(RoboBeeState.LAND);
        }
    }

    private void handleLand(VirtualRobo robo) {
        @Nullable BlockEntity port = robo.getTarget() != null ? robo.getTarget().asPortBlockEntity() : null;
        if (port == null) {
            setState(RoboBeeState.DELIVER_PACKAGE);
            return;
        }
        Vec3 end = getBelow(port, 0.5);
        Vec3 mid = getAbove(port, 1);
        Vec3 start = getAbove(port, 2);
        if (init) {
            robo.setPos(start);
            robo.setPackageHeightScale(1.0f);
            init = false;
        }
        double y = robo.getCurrentPos().y;
        double speed = (robo.getSpeed() / 20.0) / 2; // landing slower
        if (y > mid.y + speed) {
            moveTo(robo, mid, speed); // 1st part without scaling package
            robo.setPackageHeightScale(1.0f);
        } else if (y > end.y + speed) {
            moveAndScale(robo, end, speed, 1, 0); // 2nd part with scaling package
        } else {
            robo.setPos(end);
            robo.setTargetVelocity(Vec3.ZERO);
            openPort(robo.getTarget().asPortBlockEntity(), false);
            setState(RoboBeeState.DELIVER_PACKAGE);
        }
    }

    private void handleDeliverPackage(VirtualRobo robo) {
        boolean delivered = false;
        // Store target info before delivery (in case we need it after invalidation)
        Player targetPlayer = robo.getTarget() != null ? robo.getTarget().asPlayer() : null;
        BlockEntity targetPort = robo.getTarget() != null ? robo.getTarget().asPortBlockEntity() : null;

        // Try to deliver to player
        if (targetPlayer != null && !robo.getItemStack().isEmpty()) {
            delivered = BeePortBlockEntity.sendPackageToPlayer(targetPlayer, robo.getItemStack());
            if (delivered) {
                robo.setItemStack(ItemStack.EMPTY);
                robo.invalidateTarget();
            }
        }
        // Try to deliver to block entity (BeePort or AdvancedBeePort)
        if (!delivered && targetPort != null && !robo.getItemStack().isEmpty()) {
            if (targetPort instanceof BeePortBlockEntity bpbe) {
                delivered = bpbe.addItemStack(robo.getItemStack());
            } else if (targetPort instanceof AdvancedBeePortBlockEntity abpbe) {
                delivered = abpbe.addItemStack(robo.getItemStack());
            }
            if (delivered) {
                robo.setItemStack(ItemStack.EMPTY);
                robo.invalidateTarget();
            }
        }

        // If package was delivered and return-to-sender is enabled, return bee to sender (player)
        // Only applies to player-sent bees (no origin port) - port-sent bees should fly back to their origin port
        if (robo.getItemStack().isEmpty() && targetPlayer != null && robo.isBeeReturnToSender() && !robo.hasOriginPort()) {
            java.util.UUID beeFrequency = robo.getBeeFrequency();

            // Try to find and stack with an existing matching bee in the player's inventory
            boolean stacked = false;
            for (int i = 0; i < targetPlayer.getInventory().getContainerSize(); i++) {
                ItemStack invStack = targetPlayer.getInventory().getItem(i);
                if (invStack.getItem() instanceof de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem) {
                    // Check if settings match (return-to-sender mode and frequency)
                    boolean invReturnToSender = de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem.isReturnToSender(invStack);
                    java.util.UUID invFrequency = de.theidler.create_mobile_packages.items.portable_stock_ticker.LogisticallyLinkedItem.networkFromStack(invStack);
                    boolean frequenciesMatch = (beeFrequency == null && invFrequency == null) || (beeFrequency != null && beeFrequency.equals(invFrequency));
                    if (invReturnToSender && frequenciesMatch && invStack.getCount() < invStack.getMaxStackSize()) {
                        invStack.grow(1);
                        stacked = true;
                        break;
                    }
                }
            }

            // If couldn't stack, create a new bee with the correct settings
            if (!stacked) {
                ItemStack beeStack = new ItemStack(de.theidler.create_mobile_packages.index.CMPItems.ROBO_BEE.get());
                de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem.setReturnToSender(beeStack, true);
                // Set frequency if the original bee was tuned
                if (beeFrequency != null) {
                    net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                    tag.putUUID("Freq", beeFrequency);
                    beeStack.set(de.theidler.create_mobile_packages.index.CMPDataComponents.CMP_FREQ, net.minecraft.world.item.component.CustomData.of(tag));
                }
                targetPlayer.getInventory().placeItemBackInInventory(beeStack);
            }

            robo.setRemoved(robo.getServerLevel());
            return;
        }

        // If package was delivered and return-to-sender is false, bee goes into the delivery location's inventory
        if (robo.getItemStack().isEmpty() && !robo.isBeeReturnToSender()) {
            // If delivered to a port, add bee to that port's inventory
            if (targetPort != null) {
                if (targetPort instanceof BeePortBlockEntity bpbe) {
                    bpbe.addBeeToRoboBeeInventory(1);
                } else if (targetPort instanceof AdvancedBeePortBlockEntity abpbe) {
                    abpbe.addBeeToRoboBeeInventory(1);
                }
            }
            // If delivered to a player, add bee to player's inventory
            else if (targetPlayer != null) {
                java.util.UUID beeFrequency = robo.getBeeFrequency();

                // Try to find and stack with an existing matching bee (return-to-sender false, same frequency)
                boolean stacked = false;
                for (int i = 0; i < targetPlayer.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = targetPlayer.getInventory().getItem(i);
                    if (invStack.getItem() instanceof de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem) {
                        boolean invReturnToSender = de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem.isReturnToSender(invStack);
                        java.util.UUID invFrequency = de.theidler.create_mobile_packages.items.portable_stock_ticker.LogisticallyLinkedItem.networkFromStack(invStack);
                        boolean frequenciesMatch = (beeFrequency == null && invFrequency == null) || (beeFrequency != null && beeFrequency.equals(invFrequency));
                        if (!invReturnToSender && frequenciesMatch && invStack.getCount() < invStack.getMaxStackSize()) {
                            invStack.grow(1);
                            stacked = true;
                            break;
                        }
                    }
                }

                if (!stacked) {
                    ItemStack beeStack = new ItemStack(de.theidler.create_mobile_packages.index.CMPItems.ROBO_BEE.get());
                    // return-to-sender stays false
                    if (beeFrequency != null) {
                        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                        tag.putUUID("Freq", beeFrequency);
                        beeStack.set(de.theidler.create_mobile_packages.index.CMPDataComponents.CMP_FREQ, net.minecraft.world.item.component.CustomData.of(tag));
                    }
                    targetPlayer.getInventory().placeItemBackInInventory(beeStack);
                }
            }
            robo.setRemoved(robo.getServerLevel());
            return;
        }

        // updating target Address with update -> creates new target if target was null
        robo.setTargetAddress(PackageItem.getAddress(robo.getItemStack()), true);

        // if the new target is a port and the Robo is in it then shutdown the Robo.
        if (robo.getTarget() != null && robo.getTarget().asPortBlockEntity() != null) {
            if (BlockPos.containing(robo.getCurrentPos()).equals(BlockPos.containing(robo.getTargetPosition()))) {
                setState(RoboBeeState.SHUTDOWN);
                return;
            }
        }
        // else go to the new target
        setState(RoboBeeState.TAKEOFF);
    }

    private void handleShutdown(VirtualRobo robo) {
        BlockEntity blockEntity = robo.getServerLevel().getBlockEntity(BlockPos.containing(robo.getCurrentPos()));
        if (blockEntity instanceof BeePortBlockEntity bpbe) {
            bpbe.addBeeToRoboBeeInventory(1);
        } else if (blockEntity instanceof AdvancedBeePortBlockEntity abpbe) {
            abpbe.addBeeToRoboBeeInventory(1);
        }
        if (robo.getRequest() != null) {
            robo.getRequest().setStatus(RoboRequest.Status.DONE);
        }
        robo.setRemoved(robo.getServerLevel());
    }

    // Helper Functions
    private void openPort(BlockEntity port, boolean open) {
        if (port instanceof BeePortBlockEntity bpbe) {
            BeePortBlockEntity.setOpen(bpbe, open);
        } else if (port instanceof AdvancedBeePortBlockEntity abpbe) {
            AdvancedBeePortBlockEntity.setOpen(abpbe, open);
        }
    }

    private Vec3 getAbove(Object blockEntityOrPos, double y) {
        if (blockEntityOrPos instanceof BlockPos pos) {
            return pos.getCenter().add(0, y, 0);
        } else if (blockEntityOrPos instanceof BlockEntity blockEntity) {
            return blockEntity.getBlockPos().getCenter().add(0, y, 0);
        } else if (blockEntityOrPos instanceof Vec3 vec) {
            return vec.add(0, y, 0);
        }
        return Vec3.ZERO;
    }

    private Vec3 getBelow(Object blockEntityOrPos, double y) {
        if (blockEntityOrPos instanceof BlockPos pos) {
            return pos.getCenter().subtract(0, y, 0);
        } else if (blockEntityOrPos instanceof BlockEntity blockEntity) {
            return blockEntity.getBlockPos().getCenter().subtract(0, y, 0);
        } else if (blockEntityOrPos instanceof Vec3 vec) {
            return vec.subtract(0, y, 0);
        }
        return Vec3.ZERO;
    }

    private void moveTo(VirtualRobo robo, Vec3 target, double speed) {
        Vec3 dir = target.subtract(robo.getCurrentPos());
        double dist = dir.length();
        if (dist < 0.05) {
            robo.setPos(target);
            robo.setTargetVelocity(Vec3.ZERO);
        } else {
            dir = dir.normalize();
            robo.setTargetVelocity(dir.scale(speed));
        }
        // look at the target
        robo.setYaw((float) (Math.toDegrees(Math.atan2(-dir.x, dir.z))));
        robo.setPitch((float) (Math.toDegrees(Math.asin(dir.y))));
    }

    private void moveAndScale(VirtualRobo robo, Vec3 target, double speed, float scaleStart, float scaleEnd) {
        Vec3 dir = target.subtract(robo.getCurrentPos());
        double dist = dir.length();
        double totalDist = 2.0;
        float progress = (float) Math.max(0.0, Math.min(1.0, 1.0 - (dist / totalDist)));
        float scale = scaleStart + (scaleEnd - scaleStart) * progress;
        robo.setPackageHeightScale(scale);
        moveTo(robo, target, speed);
    }

    private boolean isAtTarget(VirtualRobo robo, Vec3 target, double speed) {
        return robo.getCurrentPos().distanceTo(target) < speed;
    }

    public void setState(RoboBeeState newState) {
        this.state = newState;
        this.init = true;
    }
}
