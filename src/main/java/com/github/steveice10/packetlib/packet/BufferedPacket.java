package com.github.steveice10.packetlib.packet;

import com.github.steveice10.packetlib.codec.PacketCodecHelper;
import com.github.steveice10.packetlib.codec.PacketDefinition;
import com.github.steveice10.packetlib.codec.PacketSerializer;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BufferedPacket implements Packet, PacketSerializer<BufferedPacket, PacketCodecHelper> {
    @Getter
    private final Class<? extends Packet> packetClass;
    private final byte[] buf;

    @Override
    public boolean isPriority() {
        return true;
    }

    @Override
    public void serialize(ByteBuf buf, PacketCodecHelper helper, BufferedPacket packet) {
        buf.writeBytes(this.buf);
    }

    @Override
    public BufferedPacket deserialize(ByteBuf buf, PacketCodecHelper helper, PacketDefinition<BufferedPacket, PacketCodecHelper> definition) {
        byte[] array = new byte[buf.readableBytes()];
        buf.readBytes(array);

        return new BufferedPacket(definition.packetClass(), array);
    }
}
