package org.geysermc.mcprotocollib.protocol.packet.common.serverbound;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftByteBuf;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;

@Data
@With
@AllArgsConstructor
public class ServerboundCustomPayloadPacket implements MinecraftPacket {
    private final @NonNull String channel;
    private final byte @NonNull [] data;

    public ServerboundCustomPayloadPacket(MinecraftByteBuf buf) {
        this.channel = buf.readString();
        this.data = buf.readByteArray(buf::readableBytes);
    }

    @Override
    public void serialize(MinecraftByteBuf buf) {
        buf.writeString(this.channel);
        buf.writeBytes(this.data);
    }
}
