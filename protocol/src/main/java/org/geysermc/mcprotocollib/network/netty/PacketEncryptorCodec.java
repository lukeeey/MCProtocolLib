package org.geysermc.mcprotocollib.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import org.geysermc.mcprotocollib.network.NetworkConstants;
import org.geysermc.mcprotocollib.network.crypt.EncryptionConfig;

import java.util.List;

public class PacketEncryptorCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        EncryptionConfig config = ctx.channel().attr(NetworkConstants.ENCRYPTION_ATTRIBUTE_KEY).get();
        if (config == null) {
            out.add(msg.retain());
            return;
        }

        ByteBuf heapBuf = this.ensureHeapBuffer(ctx.alloc(), msg);

        int inBytes = heapBuf.readableBytes();
        int baseOffset = heapBuf.arrayOffset() + heapBuf.readerIndex();

        try {
            config.encryption().encrypt(heapBuf.array(), baseOffset, inBytes, heapBuf.array(), baseOffset);
            out.add(heapBuf);
        } catch (Exception e) {
            heapBuf.release();
            throw new EncoderException("Error encrypting packet", e);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        EncryptionConfig config = ctx.channel().attr(NetworkConstants.ENCRYPTION_ATTRIBUTE_KEY).get();
        if (config == null) {
            out.add(in.retain());
            return;
        }

        ByteBuf heapBuf = this.ensureHeapBuffer(ctx.alloc(), in).slice();

        int inBytes = heapBuf.readableBytes();
        int baseOffset = heapBuf.arrayOffset() + heapBuf.readerIndex();

        try {
            config.encryption().decrypt(heapBuf.array(), baseOffset, inBytes, heapBuf.array(), baseOffset);
            out.add(heapBuf);
        } catch (Exception e) {
            heapBuf.release();
            throw new DecoderException("Error decrypting packet", e);
        }
    }

    private ByteBuf ensureHeapBuffer(ByteBufAllocator alloc, ByteBuf buf) {
        if (buf.hasArray()) {
            return buf.retain();
        } else {
            ByteBuf heapBuf = alloc.heapBuffer(buf.readableBytes());
            heapBuf.writeBytes(buf);
            return heapBuf;
        }
    }
}
