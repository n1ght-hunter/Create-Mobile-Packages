package de.theidler.create_mobile_packages.blocks.bee_upgrade_station;

import de.theidler.create_mobile_packages.CreateMobilePackages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class BeeUpgradeStationScreen extends AbstractContainerScreen<BeeUpgradeStationMenu> {

    private static final ResourceLocation TEXTURE = CreateMobilePackages.asResource("textures/gui/bee_upgrade_station.png");

    public BeeUpgradeStationScreen(BeeUpgradeStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Render upgrade status info
        renderUpgradeStatus(graphics, x, y);
    }

    private void renderUpgradeStatus(GuiGraphics graphics, int x, int y) {
        if (menu.hasBee()) {
            int speedUpgrades = menu.getBeeSpeedUpgrades();
            boolean hasEnder = menu.getBeeHasEnderUpgrade();

            // Display current bee status
            Component statusText;
            if (menu.isAdvancedBee()) {
                if (speedUpgrades > 0 || hasEnder) {
                    statusText = Component.translatable("create_mobile_packages.bee_upgrade_station.current_upgrades");
                    graphics.drawString(font, statusText, x + 8, y + 58, 0x404040, false);

                    int yOffset = 68;
                    if (speedUpgrades > 0) {
                        Component speedText = Component.translatable("create_mobile_packages.bee_upgrade_station.speed_count", speedUpgrades, BeeUpgradeStationBlockEntity.MAX_SPEED_UPGRADES);
                        graphics.drawString(font, speedText, x + 8, y + yOffset, 0x55FFFF, false);
                        yOffset += 10;
                    }
                    if (hasEnder) {
                        Component enderText = Component.translatable("create_mobile_packages.bee_upgrade_station.ender_installed");
                        graphics.drawString(font, enderText, x + 8, y + yOffset, 0xFF55FF, false);
                    }
                } else {
                    statusText = Component.translatable("create_mobile_packages.bee_upgrade_station.no_upgrades_yet");
                    graphics.drawString(font, statusText, x + 8, y + 58, 0x808080, false);
                }
            } else {
                statusText = Component.translatable("create_mobile_packages.bee_upgrade_station.basic_bee");
                graphics.drawString(font, statusText, x + 8, y + 58, 0x808080, false);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
