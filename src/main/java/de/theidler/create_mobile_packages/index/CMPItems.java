package de.theidler.create_mobile_packages.index;

import com.tterrag.registrate.util.entry.ItemEntry;
import de.theidler.create_mobile_packages.CreateMobilePackages;
import de.theidler.create_mobile_packages.items.portable_stock_ticker.PortableStockTicker;
import de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem;
import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager;
import de.theidler.create_mobile_packages.items.upgrades.SpeedUpgradeItem;
import de.theidler.create_mobile_packages.items.upgrades.EnderUpgradeItem;

public class CMPItems {

    public static final ItemEntry<PortableStockTicker> PORTABLE_STOCK_TICKER =
            CreateMobilePackages.REGISTRATE.item("portable_stock_ticker", PortableStockTicker::new)
                    .register();

    public static final ItemEntry<RoboBeeItem> ROBO_BEE =
            CreateMobilePackages.REGISTRATE.item("robo_bee",RoboBeeItem::new)
                    .register();

    public static final ItemEntry<MobilePackager> MOBILE_PACKAGER =
            CreateMobilePackages.REGISTRATE.item("mobile_packager", MobilePackager::new)
                    .register();

    public static final ItemEntry<SpeedUpgradeItem> SPEED_UPGRADE =
            CreateMobilePackages.REGISTRATE.item("speed_upgrade", SpeedUpgradeItem::new)
                    .register();

    public static final ItemEntry<EnderUpgradeItem> ENDER_UPGRADE =
            CreateMobilePackages.REGISTRATE.item("ender_upgrade", EnderUpgradeItem::new)
                    .register();

    public static void register() {
    }
}
