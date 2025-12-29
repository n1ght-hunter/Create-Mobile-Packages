package de.theidler.create_mobile_packages.blocks.advanced_bee_port;

import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import com.simibubi.create.content.logistics.packagePort.PackagePortScreen;
import de.theidler.create_mobile_packages.CreateMobilePackages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedBeePortScreen extends PackagePortScreen {
    public AdvancedBeePortScreen(PackagePortMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float pPartialTick, int pMouseX, int pMouseY) {
        super.renderBg(graphics, pPartialTick, pMouseX, pMouseY);
        // Render the advanced bee port GUI overlay (includes bee slot and upgrade slots)
        graphics.blit(CreateMobilePackages.asResource("textures/gui/advanced_bee_port.png"), getGuiLeft(), getGuiTop(), 0, 47, 220, 82);

        if (menu instanceof AdvancedBeePortMenu advancedBeePortMenu) {
            int eta = advancedBeePortMenu.getETA();
            Component text = advancedBeePortMenu.isBeeOnTravel()
                    ? Component.translatable("create_mobile_packages.bee_port.screen.arrival_time", eta)
                    : Component.translatable("create_mobile_packages.bee_port.screen.no_bee_on_travel");
            graphics.drawString(font, text, getGuiLeft() + 34, getGuiTop() + 64, 0x3D3C48, false);

            // Render upgrade status indicators
            if (advancedBeePortMenu.hasSpeedUpgrade()) {
                // Could add a visual indicator that speed upgrade is active
            }
            if (advancedBeePortMenu.hasEnderUpgrade()) {
                // Could add a visual indicator that ender upgrade is active
            }
        }
    }
}
