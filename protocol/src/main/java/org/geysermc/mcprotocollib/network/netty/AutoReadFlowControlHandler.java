package org.geysermc.mcprotocollib.network.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.flow.FlowControlHandler;

/**
 * A flow control handler for TCP connections.
 * When auto-read is disabled, this will halt decoding of packets until auto-read is re-enabled.
 * This is needed because auto-read still allows packets to be decoded, even if the channel is not reading anymore from the network.
 * This can happen when the channel already read a packet, but the packet is not yet decoded.
 * This will halt all decoding until the channel is ready to process more packets.
 */
public class AutoReadFlowControlHandler extends FlowControlHandler {
    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().config().isAutoRead()) {
            super.read(ctx);
        }
    }
}
