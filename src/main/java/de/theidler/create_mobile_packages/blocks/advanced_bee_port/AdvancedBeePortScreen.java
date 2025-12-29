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
            // Draw text with word wrap to avoid overlapping upgrade slots
            int maxWidth = 100;
            int x = getGuiLeft() + 34;
            int y = getGuiTop() + 58;
            for (var line : font.split(text, maxWidth)) {
                graphics.drawString(font, line, x, y, 0x3D3C48, false);
                y += font.lineHeight;
            }
        }
    }
}
