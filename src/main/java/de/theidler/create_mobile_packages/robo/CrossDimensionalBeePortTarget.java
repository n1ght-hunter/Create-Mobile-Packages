package de.theidler.create_mobile_packages.robo;

import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A RoboTarget that points to a BeePortBlockEntity in another dimension.
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
    public BlockPos asBlockPos() {
        return pos;
    }

    @Override
    public boolean isValid() {
        BeePortBlockEntity be = asBeePortBlockEntity();
        return be != null && !be.isRemoved() && !be.isFull();
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
