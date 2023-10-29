package com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;
import org.jetbrains.annotations.NotNull;

@Data
@With
@AllArgsConstructor
public class ClientboundSetPassengersPacket implements MinecraftPacket {
    private final int entityId;
    private final int @NotNull [] passengerIds;

    public ClientboundSetPassengersPacket(ByteBuf in, MinecraftCodecHelper helper) {
        this.entityId = helper.readVarInt(in);
        this.passengerIds = new int[helper.readVarInt(in)];
        for (int index = 0; index < this.passengerIds.length; index++) {
            this.passengerIds[index] = helper.readVarInt(in);
        }
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
        helper.writeVarInt(out, this.entityId);
        helper.writeVarInt(out, this.passengerIds.length);
        for (int entityId : this.passengerIds) {
            helper.writeVarInt(out, entityId);
        }
    }
}
