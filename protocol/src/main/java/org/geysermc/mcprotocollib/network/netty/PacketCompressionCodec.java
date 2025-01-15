package org.geysermc.mcprotocollib.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.network.NetworkConstants;
import org.geysermc.mcprotocollib.network.codec.PacketCodecHelper;
import org.geysermc.mcprotocollib.network.compression.CompressionConfig;

import java.util.List;

@RequiredArgsConstructor
public class PacketCompressionCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private static final int MAX_UNCOMPRESSED_SIZE = 8 * 1024 * 1024; // 8MiB
    private final PacketCodecHelper helper;

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        CompressionConfig config = ctx.channel().attr(NetworkConstants.COMPRESSION_ATTRIBUTE_KEY).get();
        if (config == null) {
            return;
        }

        config.compression().close();
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        CompressionConfig config = ctx.channel().attr(NetworkConstants.COMPRESSION_ATTRIBUTE_KEY).get();
        if (config == null) {
            out.add(msg.retain());
            return;
        }

        int uncompressed = msg.readableBytes();
        if (uncompressed > MAX_UNCOMPRESSED_SIZE) {
            throw new IllegalArgumentException("Packet too big (is " + uncompressed + ", should be less than " + MAX_UNCOMPRESSED_SIZE + ")");
        }

        ByteBuf outBuf = ctx.alloc().directBuffer(uncompressed);
        if (uncompressed < config.threshold()) {
            // Under the threshold, there is nothing to do.
            this.helper.writeVarInt(outBuf, 0);
            outBuf.writeBytes(msg);
        } else {
            this.helper.writeVarInt(outBuf, uncompressed);
            config.compression().deflate(msg, outBuf);
        }

        out.add(outBuf);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        CompressionConfig config = ctx.channel().attr(NetworkConstants.COMPRESSION_ATTRIBUTE_KEY).get();
        if (config == null) {
            out.add(in.retain());
            return;
        }

        int claimedUncompressedSize = this.helper.readVarInt(in);
        if (claimedUncompressedSize == 0) {
            out.add(in.retain());
            return;
        }

        if (config.validateDecompression()) {
            if (claimedUncompressedSize < config.threshold()) {
                throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is below server threshold of " + config.threshold());
            }

            if (claimedUncompressedSize > MAX_UNCOMPRESSED_SIZE) {
                throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is larger than protocol maximum of " + MAX_UNCOMPRESSED_SIZE);
            }
        }

        ByteBuf uncompressed = ctx.alloc().directBuffer(claimedUncompressedSize);
        try {
            config.compression().inflate(in, uncompressed, claimedUncompressedSize);
            out.add(uncompressed);
        } catch (Exception e) {
            uncompressed.release();
            throw new DecoderException("Failed to decompress packet", e);
        }
    }
}
