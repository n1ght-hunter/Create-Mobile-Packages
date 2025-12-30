package de.theidler.create_mobile_packages.robo;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.CMPHelper;
import de.theidler.create_mobile_packages.blocks.advanced_bee_port.AdvancedBeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.RoboRequest;
import de.theidler.create_mobile_packages.entities.robo_entity.RoboBeeBehaviorController;
import de.theidler.create_mobile_packages.entities.robo_entity.RoboEntity;
import de.theidler.create_mobile_packages.index.CMPEntities;
import de.theidler.create_mobile_packages.index.config.CMPConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static de.theidler.create_mobile_packages.CMPHelper.*;


public class VirtualRobo {
    private final UUID id;
    private final UUID logisticsNetworkId;
    private ItemStack itemStack;
    private Vec3 currentPos = Vec3.ZERO;
    private float yaw;
    private float pitch;
    private UUID entityId; // if a RoboEntity is spawned
    private int speed;
    private final RoboBeeBehaviorController behaviorController;
    private @Nullable RoboTarget target;
    private String targetAddress;
    private @Nullable String returnAddress = null;
    private Vec3 targetVelocity = Vec3.ZERO;
    private ServerLevel serverLevel;
    private float packageHeightScale;
    private RoboRequest request = null;
    private @Nullable ResourceKey<Level> targetDimension = null;
    private @Nullable BlockPos originPortPos = null;
    private @Nullable ResourceKey<Level> originPortDimension = null;
    private boolean canTravelDimensions = false;
    private boolean beeReturnToSender = false;
    private @Nullable UUID beeFrequency = null;

    /**
     * Basic constructor for regular bee ports.
     */
    public VirtualRobo(ServerLevel level, UUID id, ItemStack itemStack, BlockPos spawnPos,
                       UUID logisticsNetworkId, @Nullable String returnAddress) {
        this(level, id, itemStack, spawnPos, logisticsNetworkId, CMPConfigs.server().beeSpeed.get(), false, returnAddress);
    }

    /**
     * Full constructor for advanced bee ports with custom speed and cross-dimensional capability.
     */
    public VirtualRobo(ServerLevel level, UUID id, ItemStack itemStack, BlockPos spawnPos,
                       UUID logisticsNetworkId, int speed, boolean canTravelDimensions, @Nullable String returnAddress) {
        this.id = id;
        this.logisticsNetworkId = logisticsNetworkId;
        this.serverLevel = level;
        this.speed = speed;
        this.canTravelDimensions = canTravelDimensions;
        this.returnAddress = returnAddress;
        this.itemStack = itemStack;
        setTargetFromItemStack(itemStack);
        this.currentPos = spawnPos.getCenter().subtract(0, 0.5, 0);
        this.behaviorController = new RoboBeeBehaviorController();
        // Only set origin port if there's actually a port at this position
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(spawnPos);
        if (be instanceof BeePortBlockEntity || be instanceof AdvancedBeePortBlockEntity) {
            this.originPortPos = spawnPos;
            this.originPortDimension = level.dimension();
        }
    }

    public static VirtualRobo deserializeNBT(ServerLevel level, CompoundTag roboTag) {
        UUID id = roboTag.getUUID("id");
        Vec3 pos = readVec3FromTag(roboTag, "pos");
        int speed = roboTag.getInt("speed");
        UUID logisticsNetworkId = roboTag.getUUID("logisticsNetworkId");
        boolean canTravelDimensions = roboTag.getBoolean("canTravelDimensions");

        ItemStack itemStack = ItemStack.EMPTY;
        if (roboTag.contains("itemStack", Tag.TAG_COMPOUND)) {
            itemStack = ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, roboTag.get("itemStack")).result().orElse(ItemStack.EMPTY);
        }

        String returnAddress = roboTag.contains("returnAddress") ? roboTag.getString("returnAddress") : null;

        VirtualRobo virtualRobo = new VirtualRobo(level, id, itemStack, BlockPos.containing(pos), logisticsNetworkId, speed, canTravelDimensions, returnAddress);
        if (roboTag.contains("targetDimension")) {
            String dimString = roboTag.getString("targetDimension");
            virtualRobo.setTargetDimension(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimString)));
        }
        // Load origin port info
        if (roboTag.contains("originPortX")) {
            virtualRobo.originPortPos = new BlockPos(
                roboTag.getInt("originPortX"),
                roboTag.getInt("originPortY"),
                roboTag.getInt("originPortZ")
            );
        }
        if (roboTag.contains("originPortDimension")) {
            virtualRobo.originPortDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(roboTag.getString("originPortDimension")));
        }
        if (roboTag.contains("beeReturnToSender")) {
            virtualRobo.beeReturnToSender = roboTag.getBoolean("beeReturnToSender");
        }
        if (roboTag.hasUUID("beeFrequency")) {
            virtualRobo.beeFrequency = roboTag.getUUID("beeFrequency");
        }
        if (!virtualRobo.getItemStack().isEmpty()) {
            virtualRobo.setPackageHeightScale(1.0f);
        }
        return virtualRobo;
    }

    /**
     * Calculates the snap angle for a given angle. (45, 135, 225, 315)
     *
     * @param angle The angle to snap.
     * @return The snapped angle.
     */
    private int getSnapAngle(double angle) {
        return (int) Math.abs(Math.round(angle / 90) * 90 - 45);
    }

    /**
     * Calculates the angle to the current target.
     *
     * @return The angle to the target.
     */
    private double getAngleToTarget() {
        Vec3 targetPos = getTargetPosition();
        return targetPos != null ? Math.atan2(targetPos.z - this.currentPos.z, targetPos.x - this.currentPos.x()) : 0;
    }

    public @Nullable Vec3 getTargetPosition() {
        updateTarget();
        if (target == null) return null;
        return target.getTargetPos();
    }

    private void setTargetFromItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) setTargetAddress(null, false);
        else setTargetAddress(PackageItem.getAddress(itemStack), false);
    }

    private void updateTarget() {
        // if the target is still valid and in the correct network, do nothing
        if (target != null && target.isValid()) return;

        // Clear invalid target before searching for a new one
        target = null;

        // try finding a Player first
        if (canTravelDimensions) {
            // Search across all dimensions for the player
            PlayerTarget playerTarget = PlayerTarget.fromAddressAcrossDimensions(serverLevel, targetAddress);
            if (playerTarget.isValid()) {
                target = playerTarget;
                // Set target dimension to the player's dimension
                Player player = target.asPlayer();
                if (player != null && player.level() instanceof net.minecraft.server.level.ServerLevel playerLevel) {
                    targetDimension = playerLevel.dimension();
                } else {
                    targetDimension = serverLevel.dimension();
                }
                return;
            }
        } else {
            // Same dimension only
            PlayerTarget playerTarget = PlayerTarget.fromAddress(serverLevel, targetAddress);
            if (playerTarget.isValid()) {
                target = playerTarget;
                targetDimension = serverLevel.dimension();
                return;
            }
        }

        // If package was delivered and we have a return address, go there
        if (itemStack.isEmpty() && returnAddress != null && !returnAddress.isBlank()) {
            if (tryReturnToAddress()) {
                return;
            }
        }

        // Try to return to origin port
        if (originPortPos != null && originPortDimension != null) {
            if (tryOriginPort()) {
                return;
            }
        }

        // Origin port not available, find closest port
        if (tryFindPortInCurrentDimension()) {
            return;
        }

        // If cross-dimensional is enabled and no target found in current dimension, search other dimensions
        if (canTravelDimensions) {
            tryFindPortAcrossDimensions();
        }
    }

    /**
     * Tries to return to the origin port.
     * @return true if origin port was found and set as target
     */
    private boolean tryOriginPort() {
        // Check if origin port is in current dimension
        if (serverLevel.dimension().equals(originPortDimension)) {
            net.minecraft.world.level.block.entity.BlockEntity originBE = serverLevel.getBlockEntity(originPortPos);
            // Fly to the origin port even if full - bee will wait there
            if (originBE instanceof AdvancedBeePortBlockEntity abpbe && !abpbe.isRemoved()) {
                target = new AdvancedBeePortBlockEntityTarget(abpbe);
                targetDimension = serverLevel.dimension();
                return true;
            } else if (originBE instanceof BeePortBlockEntity bpbe && !bpbe.isRemoved()) {
                target = new BeePortBlockEntityTarget(bpbe);
                targetDimension = serverLevel.dimension();
                return true;
            }
        } else if (canTravelDimensions && serverLevel.getServer() != null) {
            // Origin port is in different dimension, need to teleport back
            ServerLevel originLevel = serverLevel.getServer().getLevel(originPortDimension);
            if (originLevel != null) {
                net.minecraft.world.level.block.entity.BlockEntity originBE = originLevel.getBlockEntity(originPortPos);
                if (originBE instanceof AdvancedBeePortBlockEntity abpbe && !abpbe.isRemoved()) {
                    target = new CrossDimensionalBeePortTarget(originLevel, originPortPos);
                    targetDimension = originPortDimension;
                    return true;
                } else if (originBE instanceof BeePortBlockEntity bpbe && !bpbe.isRemoved()) {
                    target = new CrossDimensionalBeePortTarget(originLevel, originPortPos);
                    targetDimension = originPortDimension;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tries to return to the return address (can be a player name or port address).
     * @return true if return target was found and set
     */
    private boolean tryReturnToAddress() {
        if (returnAddress == null || returnAddress.isBlank()) return false;

        // Try finding a player with this address first
        if (canTravelDimensions) {
            target = PlayerTarget.fromAddressAcrossDimensions(serverLevel, returnAddress);
            if (target.isValid()) {
                Player player = target.asPlayer();
                if (player != null && player.level() instanceof ServerLevel playerLevel) {
                    targetDimension = playerLevel.dimension();
                } else {
                    targetDimension = serverLevel.dimension();
                }
                return true;
            }
        } else {
            target = PlayerTarget.fromAddress(serverLevel, returnAddress);
            if (target.isValid()) {
                targetDimension = serverLevel.dimension();
                return true;
            }
        }

        // Try finding a port with this address
        BlockPos currentBlockPos = BlockPos.containing(currentPos);
        net.minecraft.world.level.block.entity.BlockEntity targetBlockEntity = CMPHelper.getClosestBeePort(serverLevel, returnAddress, currentBlockPos, this, logisticsNetworkId);
        if (targetBlockEntity == null) {
            targetBlockEntity = CMPHelper.getClosestAdvancedBeePort(serverLevel, returnAddress, currentBlockPos, this, logisticsNetworkId);
        }

        if (targetBlockEntity instanceof BeePortBlockEntity bpbe) {
            target = new BeePortBlockEntityTarget(bpbe);
            targetDimension = serverLevel.dimension();
            return true;
        } else if (targetBlockEntity instanceof AdvancedBeePortBlockEntity abpbe) {
            target = new AdvancedBeePortBlockEntityTarget(abpbe);
            targetDimension = serverLevel.dimension();
            return true;
        }

        // If cross-dimensional, search other dimensions for ports
        if (canTravelDimensions && serverLevel.getServer() != null) {
            for (ServerLevel otherLevel : serverLevel.getServer().getAllLevels()) {
                if (otherLevel.dimension().equals(serverLevel.dimension())) continue;

                targetBlockEntity = CMPHelper.getClosestBeePort(otherLevel, returnAddress, currentBlockPos, this, logisticsNetworkId);
                if (targetBlockEntity == null) {
                    targetBlockEntity = CMPHelper.getClosestAdvancedBeePort(otherLevel, returnAddress, currentBlockPos, this, logisticsNetworkId);
                }

                if (targetBlockEntity != null) {
                    target = new CrossDimensionalBeePortTarget(otherLevel, targetBlockEntity.getBlockPos());
                    targetDimension = otherLevel.dimension();
                    return true;
                }
            }
        }

        // Return address not found, clear it so we fall back to origin port
        returnAddress = null;
        return false;
    }

    /**
     * Tries to find a port in the current dimension.
     * @return true if a port was found and set as target
     */
    private boolean tryFindPortInCurrentDimension() {
        BlockPos currentBlockPos = BlockPos.containing(currentPos);

        net.minecraft.world.level.block.entity.BlockEntity targetBlockEntity = CMPHelper.getClosestBeePort(serverLevel, targetAddress, currentBlockPos, this, logisticsNetworkId);
        if (targetBlockEntity == null) {
            targetBlockEntity = CMPHelper.getClosestAdvancedBeePort(serverLevel, targetAddress, currentBlockPos, this, logisticsNetworkId);
        }

        if (targetBlockEntity instanceof BeePortBlockEntity bpbe) {
            target = new BeePortBlockEntityTarget(bpbe);
            targetDimension = serverLevel.dimension();
            return true;
        } else if (targetBlockEntity instanceof AdvancedBeePortBlockEntity abpbe) {
            target = new AdvancedBeePortBlockEntityTarget(abpbe);
            targetDimension = serverLevel.dimension();
            return true;
        }
        return false;
    }

    /**
     * Tries to find a port across all dimensions.
     */
    private void tryFindPortAcrossDimensions() {
        if (serverLevel.getServer() == null) return;

        for (ServerLevel otherLevel : serverLevel.getServer().getAllLevels()) {
            if (otherLevel.dimension().equals(serverLevel.dimension())) continue;

            BlockPos currentBlockPos = BlockPos.containing(currentPos);
            net.minecraft.world.level.block.entity.BlockEntity targetBlockEntity = CMPHelper.getClosestBeePort(otherLevel, targetAddress, currentBlockPos, this, logisticsNetworkId);
            if (targetBlockEntity == null) {
                targetBlockEntity = CMPHelper.getClosestAdvancedBeePort(otherLevel, targetAddress, currentBlockPos, this, logisticsNetworkId);
            }

            if (targetBlockEntity != null) {
                target = new CrossDimensionalBeePortTarget(otherLevel, targetBlockEntity.getBlockPos());
                targetDimension = otherLevel.dimension();
                return;
            }
        }
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setTargetVelocity(Vec3 targetVelocity) {
        if (targetVelocity == null) return;
        this.targetVelocity = targetVelocity;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        if (itemStack == null) return;
        this.itemStack = itemStack;
    }

    public void tick(ServerLevel level) {
        this.serverLevel = level;
        updateEntity();
        updateTarget();

        if (behaviorController != null) behaviorController.tick(this);
        this.move(targetVelocity);
        updateEta();

        // Spawn / despawn RoboEntity if needed
        BlockPos pos = BlockPos.containing(currentPos);
        if (level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            if (entityId == null) {
                spawnAndRememberEntity();
            }
        } else if (entityId != null) {
            despawnEntity();
        }
    }

    private void updateEta() {
        if (request != null) {
            request.setEta(calcETA(getTargetPosition(), getCurrentPos(), speed));
        } else if (target != null) {
            target.setETA(calcETA(getTargetPosition(), getCurrentPos(), speed));
        }
    }

    private void move(Vec3 targetVelocity) {
        this.currentPos = this.currentPos.add(targetVelocity);
    }

    private void updateEntity() {
        if (this.entityId != null && (serverLevel.getEntity(entityId) instanceof RoboEntity roboEntity)) {
            roboEntity.syncFromVirtual(this);
        } else {
            entityId = null;
        }
    }


    public void despawnEntity() {
        Entity entity = serverLevel.getEntity(entityId);
        if (entity != null) {
            entity.discard();
        }
        entityId = null;
    }

    private void spawnAndRememberEntity() {
        Entity entity = new RoboEntity(CMPEntities.ROBO_BEE_ENTITY.get(), serverLevel, id);
        entity.setPos(currentPos.x, currentPos.y, currentPos.z);
        serverLevel.addFreshEntity(entity);
        this.entityId = entity.getUUID();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        writeVec3ToTag(tag, "pos", currentPos);
        tag.putInt("speed", speed);
        tag.putUUID("logisticsNetworkId", logisticsNetworkId);
        tag.putBoolean("canTravelDimensions", canTravelDimensions);
        if (targetDimension != null) {
            tag.putString("targetDimension", targetDimension.location().toString());
        }
        // Save origin port info
        if (originPortPos != null) {
            tag.putInt("originPortX", originPortPos.getX());
            tag.putInt("originPortY", originPortPos.getY());
            tag.putInt("originPortZ", originPortPos.getZ());
        }
        if (originPortDimension != null) {
            tag.putString("originPortDimension", originPortDimension.location().toString());
        }
        if (returnAddress != null) {
            tag.putString("returnAddress", returnAddress);
        }
        tag.putBoolean("beeReturnToSender", beeReturnToSender);
        if (beeFrequency != null) {
            tag.putUUID("beeFrequency", beeFrequency);
        }
        if (!getItemStack().isEmpty()) {
            tag.put("itemStack", getItemStack().save(serverLevel.registryAccess(), new CompoundTag()));
        }
        return tag;
    }

    public int getSpeed() {
        return speed;
    }

    public Vec3 getCurrentPos() {
        return currentPos;
    }

    public UUID getId() {
        return id;
    }

    /**
     * Rotates the RoboEntity to a specified yaw angle.
     *
     * @param targetYaw The target yaw angle.
     * @return The number of ticks required to complete the rotation.
     */
    private int rotateToAngle(float targetYaw) {
        float currentYaw = this.yaw;
        float deltaYaw = targetYaw - currentYaw;
        deltaYaw = (deltaYaw > 180) ? deltaYaw - 360 : (deltaYaw < -180) ? deltaYaw + 360 : deltaYaw;
        float rotationSpeed = CMPConfigs.server().beeRotationSpeed.get();
        if (Math.abs(deltaYaw) > rotationSpeed) {
            currentYaw += (deltaYaw > 0) ? rotationSpeed : -rotationSpeed;
        } else {
            currentYaw = targetYaw;
        }
        this.yaw = currentYaw;
        return (int) Math.ceil(Math.abs(deltaYaw) / rotationSpeed);
    }

    public @Nullable RoboTarget getTarget() {
        return target;
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }

    public void setPos(Vec3 pos) {
        this.currentPos = pos;
    }

    /**
     * Rotates the RoboEntity to the nearest snap angle.
     *
     * @return The number of ticks required to complete the rotation.
     */
    public int rotateToSnap() {
        return rotateToAngle((float) getSnapAngle(getAngleToTarget()) + 90);
    }

    public @Nullable BeePortBlockEntity getStartBeePortBlockEntity() {
        if (serverLevel.getBlockEntity(BlockPos.containing(currentPos)) instanceof BeePortBlockEntity bpbe) {
            return bpbe;
        } else if (serverLevel.getBlockEntity(BlockPos.containing(currentPos.subtract(0,1,0))) instanceof BeePortBlockEntity bpbe) {
            return bpbe;
        } else if (serverLevel.getBlockEntity(BlockPos.containing(currentPos.subtract(0,2,0))) instanceof BeePortBlockEntity bpbe) {
            return bpbe;
        }
        return null;
    }

    /**
     * Gets the starting port block entity, which can be either a BeePortBlockEntity or AdvancedBeePortBlockEntity.
     * Returns the BlockEntity for position calculations and port operations.
     */
    public @Nullable net.minecraft.world.level.block.entity.BlockEntity getStartPortBlockEntity() {
        net.minecraft.world.level.block.entity.BlockEntity be;

        be = serverLevel.getBlockEntity(BlockPos.containing(currentPos));
        if (be instanceof BeePortBlockEntity || be instanceof AdvancedBeePortBlockEntity) return be;

        be = serverLevel.getBlockEntity(BlockPos.containing(currentPos.subtract(0,1,0)));
        if (be instanceof BeePortBlockEntity || be instanceof AdvancedBeePortBlockEntity) return be;

        be = serverLevel.getBlockEntity(BlockPos.containing(currentPos.subtract(0,2,0)));
        if (be instanceof BeePortBlockEntity || be instanceof AdvancedBeePortBlockEntity) return be;

        return null;
    }

    public void setRemoved(ServerLevel level) {
        RoboManager.get(level).remove(this.getId());
        if (request != null) {
            request.setStatus(RoboRequest.Status.DONE);
        }
        despawnEntity();
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String address, boolean update) {
        this.targetAddress = address;
        if (update) {
            updateTarget();
        }
    }

    public float getPackageHeightScale() {
        return packageHeightScale;
    }

    public void setPackageHeightScale(float scale) {
        if (scale < 0.0f || scale > 1.0f) return;
        this.packageHeightScale = scale;
    }

    public void setTarget(@Nullable RoboTarget target) {
        this.target = target;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void invalidateTarget() {
        this.targetVelocity = Vec3.ZERO;
        if (this.target != null) {
            this.target.setETA(-1); // Clear ETA when target is invalidated
        }
        this.target = null;
    }

    public boolean canTravelDimensions() {
        return canTravelDimensions;
    }

    public @Nullable ResourceKey<Level> getTargetDimension() {
        return targetDimension;
    }

    public void setTargetDimension(@Nullable ResourceKey<Level> dimension) {
        this.targetDimension = dimension;
    }

    /**
     * Checks if the robo needs to teleport to another dimension.
     */
    public boolean needsDimensionalTeleport() {
        if (!canTravelDimensions || targetDimension == null) {
            return false;
        }
        return !serverLevel.dimension().equals(targetDimension);
    }

    /**
     * Sets the server level (used during dimensional teleport).
     */
    public void setServerLevel(ServerLevel level) {
        this.serverLevel = level;
    }

    public UUID getLogisticsNetworkId() {
        return logisticsNetworkId;
    }

    public RoboRequest getRequest() {
        return request;
    }

    public void setRequest(RoboRequest request) {
        this.request = request;
        this.request.setStatus(RoboRequest.Status.IN_PROGRESS);
        this.target = new BeePortBlockEntityTarget((BeePortBlockEntity) serverLevel.getBlockEntity(request.getTargetPos()));
    }

    public boolean isBeeReturnToSender() {
        return beeReturnToSender;
    }

    public void setBeeReturnToSender(boolean beeReturnToSender) {
        this.beeReturnToSender = beeReturnToSender;
    }

    public @Nullable UUID getBeeFrequency() {
        return beeFrequency;
    }

    public void setBeeFrequency(@Nullable UUID beeFrequency) {
        this.beeFrequency = beeFrequency;
    }

    /**
     * Checks if this robo was sent from a port (has an origin port to return to).
     * @return true if the robo has an origin port, false if it was sent from a player
     */
    public boolean hasOriginPort() {
        return originPortPos != null;
    }
}
