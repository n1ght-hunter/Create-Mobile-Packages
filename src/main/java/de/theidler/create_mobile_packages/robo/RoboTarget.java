package de.theidler.create_mobile_packages.robo;

import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public interface RoboTarget {
    Vec3 getTargetPos();

    default BeePortBlockEntity asBeePortBlockEntity() {
        return null;
    }

    /**
     * Returns the target as a generic BlockEntity (for both BeePort and AdvancedBeePort).
     */
    default BlockEntity asPortBlockEntity() {
        return asBeePortBlockEntity();
    }

    default Player asPlayer() {
        return null;
    }

    default BlockPos asBlockPos() {
        return null;
    }

    default boolean isValid() {
        return true;
    }

    void setETA(int eta);

    int getETA();
}

