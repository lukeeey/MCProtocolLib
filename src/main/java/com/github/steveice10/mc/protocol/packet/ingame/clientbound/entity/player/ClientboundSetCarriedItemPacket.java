package com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;

@Data
@With
@AllArgsConstructor
public class ClientboundSetCarriedItemPacket implements MinecraftPacket {
    private final int slot;

    public ClientboundSetCarriedItemPacket(ByteBuf in, MinecraftCodecHelper helper) {
        this.slot = in.readByte();
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
        out.writeByte(this.slot);
    }
}
