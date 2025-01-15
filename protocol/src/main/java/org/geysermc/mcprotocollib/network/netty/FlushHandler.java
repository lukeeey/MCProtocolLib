package org.geysermc.mcprotocollib.network.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * Sending a {@link FlushPacket} will ensure all before were sent.
 * This handler makes sure it's dropped before it reaches the encoder.
 * This logic is similar to the Minecraft UnconfiguredPipelineHandler.OutboundConfigurationTask.
 */
public class FlushHandler extends ChannelOutboundHandlerAdapter {
    public static final FlushPacket FLUSH_PACKET = new FlushPacket();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == FLUSH_PACKET) {
            promise.setSuccess();
        } else {
            super.write(ctx, msg, promise);
        }
    }

    public static class FlushPacket {
        private FlushPacket() {
        }
    }
}
