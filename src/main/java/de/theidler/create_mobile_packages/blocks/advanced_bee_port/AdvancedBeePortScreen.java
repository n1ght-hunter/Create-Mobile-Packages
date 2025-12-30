package de.theidler.create_mobile_packages.blocks.advanced_bee_port;

import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import com.simibubi.create.content.logistics.packagePort.PackagePortScreen;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import de.theidler.create_mobile_packages.CreateMobilePackages;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedBeePortScreen extends PackagePortScreen {

    private IconButton returnToSenderButton;

    public AdvancedBeePortScreen(PackagePortMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void init() {
        super.init();

        if (menu instanceof AdvancedBeePortMenu advancedBeePortMenu) {
            returnToSenderButton = new IconButton(getGuiLeft() + 118, getGuiTop() + 56, AllIcons.I_REFRESH);
            returnToSenderButton.withCallback(this::toggleReturnToSender);
            returnToSenderButton.setToolTip(advancedBeePortMenu.isReturnToSender()
                    ? Component.translatable("create_mobile_packages.advanced_bee_port.return_to_sender.enabled")
                    : Component.translatable("create_mobile_packages.advanced_bee_port.return_to_sender.disabled"));
            returnToSenderButton.green = advancedBeePortMenu.isReturnToSender();
            addRenderableWidget(returnToSenderButton);
        }
    }

    private void toggleReturnToSender() {
        if (menu instanceof AdvancedBeePortMenu advancedBeePortMenu) {
            CatnipServices.NETWORK.sendToServer(new ToggleAdvancedPortReturnToSenderPacket(advancedBeePortMenu.getBlockPos()));
            returnToSenderButton.green = !returnToSenderButton.green;
            returnToSenderButton.setToolTip(returnToSenderButton.green
                    ? Component.translatable("create_mobile_packages.advanced_bee_port.return_to_sender.enabled")
                    : Component.translatable("create_mobile_packages.advanced_bee_port.return_to_sender.disabled"));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float pPartialTick, int pMouseX, int pMouseY) {
        super.renderBg(graphics, pPartialTick, pMouseX, pMouseY);
        graphics.blit(CreateMobilePackages.asResource("textures/gui/advanced_bee_port.png"), getGuiLeft(), getGuiTop(), 0, 47, 220, 82);

        if (menu instanceof AdvancedBeePortMenu advancedBeePortMenu) {
            int eta = advancedBeePortMenu.getETA();
            Component text = advancedBeePortMenu.isBeeOnTravel()
                    ? Component.translatable("create_mobile_packages.bee_port.screen.arrival_time", eta)
                    : Component.translatable("create_mobile_packages.bee_port.screen.no_bee_on_travel");
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
