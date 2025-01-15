package org.geysermc.mcprotocollib.network.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBundlePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDelimiterPacket;

import java.util.ArrayList;
import java.util.List;

public class BundlerUnpackerDecoder extends MessageToMessageDecoder<MinecraftPacket> {
    private List<MinecraftPacket> currentPackets;

    @Override
    protected void decode(ChannelHandlerContext ctx, MinecraftPacket packet, List<Object> out) {
        if (currentPackets != null) {
            if (packet.getClass() == ClientboundDelimiterPacket.class) {
                out.add(new ClientboundBundlePacket(currentPackets));
                currentPackets = null;
            } else {
                currentPackets.add(packet);
            }
        } else {
            if (packet.getClass() == ClientboundDelimiterPacket.class) {
                currentPackets = new ArrayList<>(2);
            }
        }
    }
}
