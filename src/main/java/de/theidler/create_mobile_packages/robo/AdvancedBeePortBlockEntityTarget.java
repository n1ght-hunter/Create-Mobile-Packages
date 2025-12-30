package de.theidler.create_mobile_packages.robo;

import de.theidler.create_mobile_packages.blocks.advanced_bee_port.AdvancedBeePortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class AdvancedBeePortBlockEntityTarget implements RoboTarget {
    private final BlockPos pos;
    private final ServerLevel level;
    private int eta = -1;

    public AdvancedBeePortBlockEntityTarget(AdvancedBeePortBlockEntity be) {
        this.pos = be.getBlockPos();
        this.level = be.getLevel() instanceof ServerLevel ? (ServerLevel) be.getLevel() : null;
    }

    @Override
    public Vec3 getTargetPos() {
        return Vec3.atCenterOf(pos);
    }

    public AdvancedBeePortBlockEntity asAdvancedBeePortBlockEntity() {
        // Lazy lookup to avoid holding onto a removed/invalid instance
        if (level == null) return null;
        if (level.getBlockEntity(pos) instanceof AdvancedBeePortBlockEntity be) return be;
        return null;
    }

    @Override
    public BlockEntity asPortBlockEntity() {
        return asAdvancedBeePortBlockEntity();
    }

    @Override
    public boolean isValid() {
        AdvancedBeePortBlockEntity be = asAdvancedBeePortBlockEntity();
        // Don't check isFull() - bee should fly to port and wait if full
        return be != null && !be.isRemoved();
    }

    @Override
    public int getETA() {
        return eta;
    }

    @Override
    public void setETA(int eta) {
        this.eta = eta;
    }
}
