package de.theidler.create_mobile_packages.blocks.advanced_bee_port;

import de.theidler.create_mobile_packages.index.CMPPackets;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ToggleAdvancedPortReturnToSenderPacket implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleAdvancedPortReturnToSenderPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, packet -> packet.pos,
                    ToggleAdvancedPortReturnToSenderPacket::new
            );

    private final BlockPos pos;

    public ToggleAdvancedPortReturnToSenderPacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void handle(ServerPlayer player) {
        if (player == null || !player.isAlive()) return;
        if (player.level() == null) return;

        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof AdvancedBeePortBlockEntity advancedPort) {
            // Verify player can interact with this block
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64) {
                advancedPort.toggleReturnToSender();
            }
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CMPPackets.TOGGLE_ADVANCED_PORT_RETURN_TO_SENDER;
    }
}
