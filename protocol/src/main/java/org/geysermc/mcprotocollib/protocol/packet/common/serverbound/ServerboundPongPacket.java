package org.geysermc.mcprotocollib.protocol.packet.common.serverbound;

import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;

@Data
@With
@AllArgsConstructor
public class ServerboundPongPacket implements MinecraftPacket {
    private final int id;

    public ServerboundPongPacket(ByteBuf in, MinecraftCodecHelper helper) {
        this.id = in.readInt();
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
        out.writeInt(this.id);
    }
}