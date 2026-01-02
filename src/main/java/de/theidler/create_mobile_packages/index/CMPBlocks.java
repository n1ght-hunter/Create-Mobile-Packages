package de.theidler.create_mobile_packages.index;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import de.theidler.create_mobile_packages.CreateMobilePackages;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlock;
import de.theidler.create_mobile_packages.blocks.bee_upgrade_station.BeeUpgradeStationBlock;

import static com.simibubi.create.api.behaviour.display.DisplaySource.displaySource;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;


public class CMPBlocks {

    public static final BlockEntry<BeePortBlock> BEE_PORT = CreateMobilePackages.REGISTRATE.block("bee_port", BeePortBlock::new)
                    .initialProperties(SharedProperties::wooden)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .transform(displaySource(CMPDisplaySources.BEE_COUNT))
                    .transform(displaySource(CMPDisplaySources.BEE_ETA))
                    .item(LogisticallyLinkedBlockItem::new)
                    .transform(customItemModel())
                    .register();

    public static final BlockEntry<BeeUpgradeStationBlock> BEE_UPGRADE_STATION = CreateMobilePackages.REGISTRATE.block("bee_upgrade_station", BeeUpgradeStationBlock::new)
                    .initialProperties(SharedProperties::wooden)
                    .simpleItem()
                    .register();

    public static void register() {
    }
}
