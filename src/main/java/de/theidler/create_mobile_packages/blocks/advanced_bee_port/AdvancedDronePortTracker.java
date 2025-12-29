package de.theidler.create_mobile_packages.blocks.advanced_bee_port;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdvancedDronePortTracker extends SavedData {
    private final List<AdvancedBeePortBlockEntity> dronePorts = new ArrayList<>();

    public static AdvancedDronePortTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(AdvancedDronePortTracker::create, AdvancedDronePortTracker::load), "advanced_drone_port_tracker");
    }

    public static AdvancedDronePortTracker create() {
        return new AdvancedDronePortTracker();
    }

    public static AdvancedDronePortTracker load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        return AdvancedDronePortTracker.create();
    }

    public void add(AdvancedBeePortBlockEntity dronePort) {
        if (!dronePorts.contains(dronePort)) {
            dronePorts.add(dronePort);
            setDirty();
        }
    }

    public void remove(AdvancedBeePortBlockEntity dronePort) {
        if (dronePorts.remove(dronePort)) {
            setDirty();
        }
    }

    public List<AdvancedBeePortBlockEntity> getAll() {
        return dronePorts;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        return compoundTag;
    }

    public List<AdvancedBeePortBlockEntity> getAllByNetwork(UUID logisticsNetworkId) {
        return dronePorts.stream().filter(dpbe -> dpbe.getLogisticsNetworkId().equals(logisticsNetworkId)).toList();
    }
}
