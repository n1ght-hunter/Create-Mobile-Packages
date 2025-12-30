package de.theidler.create_mobile_packages.items.robo_bee;

import de.theidler.create_mobile_packages.index.CMPPackets;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ToggleReturnToSenderPacket implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleReturnToSenderPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, packet -> packet.mainHand,
                    ToggleReturnToSenderPacket::new
            );

    private final boolean mainHand;

    public ToggleReturnToSenderPacket(boolean mainHand) {
        this.mainHand = mainHand;
    }

    @Override
    public void handle(ServerPlayer player) {
        if (player == null || !player.isAlive()) return;

        InteractionHand hand = mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof RoboBeeItem) {
            RoboBeeItem.onScrollToggle(player, stack);
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CMPPackets.TOGGLE_RETURN_TO_SENDER;
    }
}
