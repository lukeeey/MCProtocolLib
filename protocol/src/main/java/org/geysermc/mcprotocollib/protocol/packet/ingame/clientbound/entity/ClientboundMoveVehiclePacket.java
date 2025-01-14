package org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;

@Data
@With
@AllArgsConstructor
public class ClientboundMoveVehiclePacket implements MinecraftPacket {
    private final Vector3d position;
    private final float yRot;
    private final float xRot;

    public ClientboundMoveVehiclePacket(ByteBuf in, MinecraftCodecHelper helper) {
        this.position = Vector3d.from(in.readDouble(), in.readDouble(), in.readDouble());
        this.yRot = in.readFloat();
        this.xRot = in.readFloat();
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
        out.writeDouble(this.position.getX());
        out.writeDouble(this.position.getY());
        out.writeDouble(this.position.getZ());
        out.writeFloat(this.yRot);
        out.writeFloat(this.xRot);
    }

    @Override
    public boolean shouldRunOnGameThread() {
        return true;
    }
}
