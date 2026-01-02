package de.theidler.create_mobile_packages.index;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import de.theidler.create_mobile_packages.CreateMobilePackages;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_upgrade_station.BeeUpgradeStationBlockEntity;

public class CMPBlockEntities {

    public static final BlockEntityEntry<BeePortBlockEntity> BEE_PORT = CreateMobilePackages.REGISTRATE
            .blockEntity("bee_port", BeePortBlockEntity::new)
            .validBlocks(CMPBlocks.BEE_PORT)
            .register();

    public static final BlockEntityEntry<BeeUpgradeStationBlockEntity> BEE_UPGRADE_STATION = CreateMobilePackages.REGISTRATE
            .blockEntity("bee_upgrade_station", BeeUpgradeStationBlockEntity::new)
            .validBlocks(CMPBlocks.BEE_UPGRADE_STATION)
            .register();

    public static void register() {
    }
}
