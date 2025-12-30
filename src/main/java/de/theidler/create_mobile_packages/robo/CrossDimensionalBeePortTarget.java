package de.theidler.create_mobile_packages.robo;

import de.theidler.create_mobile_packages.blocks.advanced_bee_port.AdvancedBeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A RoboTarget that points to a BeePortBlockEntity or AdvancedBeePortBlockEntity in another dimension.
 * Used by the Ender Upgrade for cross-dimensional delivery.
 */
public class CrossDimensionalBeePortTarget implements RoboTarget {
    private final BlockPos pos;
    private final ServerLevel level;
    private final ResourceKey<Level> dimension;
    private int eta;

    public CrossDimensionalBeePortTarget(ServerLevel level, BlockPos pos) {
        this.pos = pos;
        this.level = level;
        this.dimension = level.dimension();
    }

    @Override
    public Vec3 getTargetPos() {
        return Vec3.atCenterOf(pos);
    }

    @Override
    public BeePortBlockEntity asBeePortBlockEntity() {
        if (level == null) return null;
        if (level.getBlockEntity(pos) instanceof BeePortBlockEntity be) return be;
        return null;
    }

    @Override
    public BlockEntity asPortBlockEntity() {
        if (level == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BeePortBlockEntity || be instanceof AdvancedBeePortBlockEntity) {
            return be;
        }
        return null;
    }

    @Override
    public BlockPos asBlockPos() {
        return pos;
    }

    @Override
    public boolean isValid() {
        BlockEntity be = asPortBlockEntity();
        if (be instanceof BeePortBlockEntity bpbe) {
            return !bpbe.isRemoved() && !bpbe.isFull();
        } else if (be instanceof AdvancedBeePortBlockEntity abpbe) {
            return !abpbe.isRemoved() && !abpbe.isFull();
        }
        return false;
    }

    @Override
    public int getETA() {
        return eta;
    }

    @Override
    public void setETA(int eta) {
        this.eta = eta;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public ServerLevel getLevel() {
        return level;
    }
}
