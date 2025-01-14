package org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerboundPlayerLoadedPacket implements MinecraftPacket {
    public static final ServerboundPlayerLoadedPacket INSTANCE = new ServerboundPlayerLoadedPacket();

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
    }

    @Override
    public boolean shouldRunOnGameThread() {
        return true;
    }
}
