package org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.KnownPack;

import java.util.ArrayList;
import java.util.List;

@Data
@With
@AllArgsConstructor
public class ServerboundSelectKnownPacks implements MinecraftPacket {
    private final List<KnownPack> knownPacks;

    public ServerboundSelectKnownPacks(ByteBuf in) {
        this.knownPacks = KnownPack.NETWORK_CODEC.list(64).read(in);
    }

    @Override
    public void serialize(ByteBuf out) {
        KnownPack.NETWORK_CODEC.list(64).write(out, knownPacks);
    }

    @Override
    public boolean shouldRunOnGameThread() {
        return true;
    }
}
