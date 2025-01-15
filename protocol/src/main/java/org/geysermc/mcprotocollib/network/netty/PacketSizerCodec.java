package org.geysermc.mcprotocollib.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.network.codec.PacketCodecHelper;
import org.geysermc.mcprotocollib.network.packet.PacketHeader;

import java.util.List;

@RequiredArgsConstructor
public class PacketSizerCodec extends ByteToMessageCodec<ByteBuf> {
    private final PacketHeader header;
    private final PacketCodecHelper codecHelper;

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
        int size = header.getLengthSize();
        if (size == 0) {
            out.writeBytes(in);
            return;
        }

        int length = in.readableBytes();
        out.ensureWritable(header.getLengthSize(length) + length);
        header.writeLength(out, codecHelper, length);
        out.writeBytes(in);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        int size = header.getLengthSize();
        if (size == 0) {
            out.add(buf.retain());
            return;
        }

        buf.markReaderIndex();
        byte[] lengthBytes = new byte[size];
        for (int index = 0; index < lengthBytes.length; index++) {
            if (!buf.isReadable()) {
                buf.resetReaderIndex();
                return;
            }

            lengthBytes[index] = buf.readByte();
            if ((header.isLengthVariable() && lengthBytes[index] >= 0) || index == size - 1) {
                int length = header.readLength(Unpooled.wrappedBuffer(lengthBytes), codecHelper, buf.readableBytes());
                if (buf.readableBytes() < length) {
                    buf.resetReaderIndex();
                    return;
                }

                out.add(buf.readBytes(length));
                return;
            }
        }

        throw new CorruptedFrameException("Length is too long.");
    }
}
