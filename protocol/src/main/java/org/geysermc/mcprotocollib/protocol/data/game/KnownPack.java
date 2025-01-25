package org.geysermc.mcprotocollib.protocol.data.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.codec.NetworkCodec;

@Data
@AllArgsConstructor
public class KnownPack {
    public static NetworkCodec<KnownPack> NETWORK_CODEC = NetworkCodec.of((buf, knownPack) -> {
        MinecraftTypes.writeString(buf, knownPack.getNamespace());
        MinecraftTypes.writeString(buf, knownPack.getId());
        MinecraftTypes.writeString(buf, knownPack.getVersion());
    }, buf -> {
        String namespace = MinecraftTypes.readString(buf);
        String id = MinecraftTypes.readString(buf);
        String version = MinecraftTypes.readString(buf);
        return new KnownPack(namespace, id, version);
    });

    private String namespace;
    private String id;
    private String version;
}
