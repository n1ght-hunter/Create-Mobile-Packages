package de.theidler.create_mobile_packages.blocks.bee_port;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Global tracker for bee ports across all dimensions.
 * Used by the Ender Upgrade to find delivery targets in other dimensions.
 */
public class GlobalDronePortTracker {

    private static MinecraftServer server;

    public static void setServer(MinecraftServer server) {
        GlobalDronePortTracker.server = server;
    }

    public static void clear() {
        GlobalDronePortTracker.server = null;
    }

    public static MinecraftServer getServer() {
        return server;
    }

    /**
     * Finds all bee ports across all dimensions that match the given criteria.
     *
     * @param address            The address to filter by (null for no filter)
     * @param logisticsNetworkId The logistics network ID to filter by
     * @param excludeDimension   The dimension to exclude from search (usually the origin dimension)
     * @return List of matching cross-dimensional targets
     */
    public static List<CrossDimensionalTarget> findPortsAcrossDimensions(
            String address,
            UUID logisticsNetworkId,
            ResourceKey<Level> excludeDimension) {

        List<CrossDimensionalTarget> results = new ArrayList<>();

        if (server == null) {
            return results;
        }

        for (ServerLevel level : server.getAllLevels()) {
            // Skip the excluded dimension (origin dimension)
            if (level.dimension().equals(excludeDimension)) {
                continue;
            }

            DronePortTracker tracker = DronePortTracker.get(level);
            List<BeePortBlockEntity> ports = tracker.getAllByNetwork(logisticsNetworkId);

            for (BeePortBlockEntity port : ports) {
                if (port.isRemoved() || port.isFull()) {
                    continue;
                }

                // Check address filter if provided
                if (address != null && !address.isEmpty()) {
                    if (!PackageItem.matchAddress(address, port.addressFilter)) {
                        continue;
                    }
                }

                results.add(new CrossDimensionalTarget(
                        level.dimension(),
                        port.getBlockPos(),
                        level
                ));
            }
        }

        return results;
    }

    /**
     * Finds the closest bee port across all dimensions.
     *
     * @param address            The address to filter by
     * @param logisticsNetworkId The logistics network ID
     * @param originPos          The origin position (for distance calculation within same dimension)
     * @param excludeDimension   The dimension to exclude
     * @return The closest matching target, or null if none found
     */
    public static CrossDimensionalTarget findClosestPortAcrossDimensions(
            String address,
            UUID logisticsNetworkId,
            BlockPos originPos,
            ResourceKey<Level> excludeDimension) {

        List<CrossDimensionalTarget> targets = findPortsAcrossDimensions(address, logisticsNetworkId, excludeDimension);

        if (targets.isEmpty()) {
            return null;
        }

        // For cross-dimensional, we just return the first valid target
        // since distance comparisons across dimensions don't make sense
        return targets.get(0);
    }

    /**
     * Represents a bee port target in another dimension.
     */
    public record CrossDimensionalTarget(
            ResourceKey<Level> dimension,
            BlockPos pos,
            ServerLevel level
    ) {
    }
}
