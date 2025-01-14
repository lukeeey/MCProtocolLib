package org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;

@Data
@With
@AllArgsConstructor
public class ClientboundLevelParticlesPacket implements MinecraftPacket {
    private final @NonNull Particle particle;
    private final boolean longDistance;
    private final boolean alwaysShow;
    private final double x;
    private final double y;
    private final double z;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float velocityOffset;
    private final int amount;

    public ClientboundLevelParticlesPacket(ByteBuf in, MinecraftCodecHelper helper) {
        this.longDistance = in.readBoolean();
        this.alwaysShow = in.readBoolean();
        this.x = in.readDouble();
        this.y = in.readDouble();
        this.z = in.readDouble();
        this.offsetX = in.readFloat();
        this.offsetY = in.readFloat();
        this.offsetZ = in.readFloat();
        this.velocityOffset = in.readFloat();
        this.amount = in.readInt();
        ParticleType type = helper.readParticleType(in);
        this.particle = new Particle(type, helper.readParticleData(in, type));
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
        out.writeBoolean(this.longDistance);
        out.writeBoolean(this.alwaysShow);
        out.writeDouble(this.x);
        out.writeDouble(this.y);
        out.writeDouble(this.z);
        out.writeFloat(this.offsetX);
        out.writeFloat(this.offsetY);
        out.writeFloat(this.offsetZ);
        out.writeFloat(this.velocityOffset);
        out.writeInt(this.amount);
        helper.writeParticleType(out, this.particle.getType());
        helper.writeParticleData(out, this.particle.getType(), this.particle.getData());
    }

    @Override
    public boolean shouldRunOnGameThread() {
        return true;
    }
}
