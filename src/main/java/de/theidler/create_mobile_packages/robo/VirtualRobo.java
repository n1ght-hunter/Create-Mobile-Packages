package de.theidler.create_mobile_packages.robo;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.CMPHelper;
import de.theidler.create_mobile_packages.blocks.advanced_bee_port.AdvancedBeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.RoboRequest;
import de.theidler.create_mobile_packages.blocks.bee_port.GlobalDronePortTracker;
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
    private Vec3 targetVelocity = Vec3.ZERO;
    private ServerLevel serverLevel;
    private float packageHeightScale;
    private RoboRequest request = null;
    private boolean crossDimensionalEnabled = false;
    private @Nullable ResourceKey<Level> targetDimension = null;

    public VirtualRobo(ServerLevel level, UUID id, ItemStack itemStack, BlockPos spawnPos, UUID logisticsNetworkId) {
        this.id = id;
        this.logisticsNetworkId = logisticsNetworkId;
        this.serverLevel = level;
        this.speed = CMPConfigs.server().beeSpeed.get();
        this.itemStack = itemStack;
        setTargetFromItemStack(itemStack);
        this.currentPos = spawnPos.getCenter().subtract(0, 0.5, 0);
        this.behaviorController = new RoboBeeBehaviorController();
    }

    /**
     * Constructor for advanced robo with custom speed and cross-dimensional capability.
     */
    public VirtualRobo(ServerLevel level, UUID id, ItemStack itemStack, BlockPos spawnPos,
                       UUID logisticsNetworkId, int speed, boolean crossDimensional) {
        this(level, id, itemStack, spawnPos, logisticsNetworkId);
        this.speed = speed;
        this.crossDimensionalEnabled = crossDimensional;
    }

    public static VirtualRobo deserializeNBT(ServerLevel level, CompoundTag roboTag) {
        UUID id = roboTag.getUUID("id");
        Vec3 pos = readVec3FromTag(roboTag, "pos");
        int speed = roboTag.getInt("speed");
        UUID logisticsNetworkId = roboTag.getUUID("logisticsNetworkId");
        boolean crossDimensional = roboTag.getBoolean("crossDimensional");

        ItemStack itemStack = ItemStack.EMPTY;
        if (roboTag.contains("itemStack", Tag.TAG_COMPOUND)) {
            itemStack = ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, roboTag.get("itemStack")).result().orElse(ItemStack.EMPTY);
        }

        VirtualRobo virtualRobo = new VirtualRobo(level, id, itemStack, BlockPos.containing(pos), logisticsNetworkId);
        virtualRobo.setSpeed(speed);
        virtualRobo.setCrossDimensionalEnabled(crossDimensional);
        if (roboTag.contains("targetDimension")) {
            String dimString = roboTag.getString("targetDimension");
            virtualRobo.setTargetDimension(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimString)));
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

        // try finding a Player first (same dimension only)
        target = PlayerTarget.fromAddress(serverLevel, targetAddress);
        if (target.isValid()) {
            targetDimension = serverLevel.dimension();
            return;
        }

        // if no player found, try finding any port (BeePort or AdvancedBeePort) within the network (same dimension)
        net.minecraft.world.level.block.entity.BlockEntity targetBlockEntity = CMPHelper.getClosestAnyPort(serverLevel, targetAddress, BlockPos.containing(currentPos), this, logisticsNetworkId);
        if (targetBlockEntity instanceof BeePortBlockEntity bpbe) {
            target = new BeePortBlockEntityTarget(bpbe);
            targetDimension = serverLevel.dimension();
            return;
        } else if (targetBlockEntity instanceof AdvancedBeePortBlockEntity abpbe) {
            target = new AdvancedBeePortBlockEntityTarget(abpbe);
            targetDimension = serverLevel.dimension();
            return;
        }

        // If cross-dimensional is enabled and no target found in current dimension, search other dimensions
        if (crossDimensionalEnabled && CMPConfigs.server().enderUpgradeEnabled.get()) {
            GlobalDronePortTracker.CrossDimensionalTarget crossDimTarget =
                    GlobalDronePortTracker.findClosestPortAcrossDimensions(
                            targetAddress,
                            logisticsNetworkId,
                            BlockPos.containing(currentPos),
                            serverLevel.dimension()
                    );

            if (crossDimTarget != null) {
                target = new CrossDimensionalBeePortTarget(crossDimTarget.level(), crossDimTarget.pos());
                targetDimension = crossDimTarget.dimension();
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
            request.setEta(calcETA(getTargetPosition(), getCurrentPos()));
        } else if (target != null) {
            target.setETA(calcETA(getTargetPosition(), getCurrentPos()));
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
        tag.putBoolean("crossDimensional", crossDimensionalEnabled);
        if (targetDimension != null) {
            tag.putString("targetDimension", targetDimension.location().toString());
        }
        if (!getItemStack().isEmpty()) {
            tag.put("itemStack", getItemStack().save(serverLevel.registryAccess(), new CompoundTag()));
        }
        return tag;
    }

    private void setSpeed(int speed) {
        this.speed = speed;
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
        if (request != null && request.getStatus() == RoboRequest.Status.IN_PROGRESS) {
            request.setStatus(RoboRequest.Status.PENDING);
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
        this.target = null;
    }

    // Cross-dimensional support methods
    public boolean isCrossDimensionalEnabled() {
        return crossDimensionalEnabled;
    }

    public void setCrossDimensionalEnabled(boolean enabled) {
        this.crossDimensionalEnabled = enabled;
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
        if (!crossDimensionalEnabled || targetDimension == null) {
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
}
