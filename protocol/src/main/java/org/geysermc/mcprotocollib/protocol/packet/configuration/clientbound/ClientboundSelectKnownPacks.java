package org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.KnownPack;

import java.util.List;

@Data
@With
@AllArgsConstructor
public class ClientboundSelectKnownPacks implements MinecraftPacket {
    private final List<KnownPack> knownPacks;

    public ClientboundSelectKnownPacks(ByteBuf in) {
        this.knownPacks = KnownPack.NETWORK_CODEC.list().read(in);
    }

    @Override
    public void serialize(ByteBuf out) {
        KnownPack.NETWORK_CODEC.list().write(out, knownPacks);
    }

    @Override
    public boolean shouldRunOnGameThread() {
        return true;
    }
}
