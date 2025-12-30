package de.theidler.create_mobile_packages;

import de.theidler.create_mobile_packages.entities.models.RoboBeeModel;
import de.theidler.create_mobile_packages.entities.render.DroneEntityRenderer;
import de.theidler.create_mobile_packages.index.CMPEntities;
import de.theidler.create_mobile_packages.index.ponder.CMPPonderPlugin;
import de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem;
import de.theidler.create_mobile_packages.items.robo_bee.ToggleReturnToSenderPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CreateMobilePackages.MODID, dist = Dist.CLIENT)
public class CreateMobilePackagesClient {

    public CreateMobilePackagesClient(IEventBus modEventBus) {
        onCtorClient(modEventBus);
    }

    public static void onCtorClient(IEventBus modEventBus) {
        modEventBus.addListener(CreateMobilePackagesClient::clientInit);
        modEventBus.addListener(CreateMobilePackagesClient::registerEntityRenderers);
        modEventBus.addListener(CreateMobilePackagesClient::registerLayerDefinitions);

        // Register game event handlers
        NeoForge.EVENT_BUS.addListener(CreateMobilePackagesClient::onMouseScroll);
    }

    private static void clientInit(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new CMPPonderPlugin());
    }

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CMPEntities.ROBO_BEE_ENTITY.get(), DroneEntityRenderer::new);
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RoboBeeModel.LAYER_LOCATION, RoboBeeModel::createBodyLayer);
    }

    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isShiftKeyDown()) return;
        if (mc.screen != null) return; // Don't trigger when a screen is open

        // Check if holding a Robo Bee in either hand
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        boolean mainHandHasBee = mainHand.getItem() instanceof RoboBeeItem;
        boolean offHandHasBee = offHand.getItem() instanceof RoboBeeItem;

        if (mainHandHasBee) {
            CatnipServices.NETWORK.sendToServer(new ToggleReturnToSenderPacket(true));
            event.setCanceled(true);
        } else if (offHandHasBee) {
            CatnipServices.NETWORK.sendToServer(new ToggleReturnToSenderPacket(false));
            event.setCanceled(true);
        }
    }
}
