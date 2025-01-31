package org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;

@Data
@With
@AllArgsConstructor
public class ServerboundJigsawGeneratePacket implements MinecraftPacket {
    private final @NonNull Vector3i position;
    private final int levels;
    private final boolean keepJigsaws;

    public ServerboundJigsawGeneratePacket(ByteBuf in) {
        this.position = MinecraftTypes.readPosition(in);
        this.levels = MinecraftTypes.readVarInt(in);
        this.keepJigsaws = in.readBoolean();
    }

    @Override
    public void serialize(ByteBuf out) {
        MinecraftTypes.writePosition(out, this.position);
        MinecraftTypes.writeVarInt(out, this.levels);
        out.writeBoolean(this.keepJigsaws);
    }

    @Override
    public boolean shouldRunOnGameThread() {
        return true;
    }
}
