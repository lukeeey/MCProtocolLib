package org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftByteBuf;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;

@Data
@With
@AllArgsConstructor
public class ClientboundSetTitleTextPacket implements MinecraftPacket {
    private final @NonNull Component text;

    public ClientboundSetTitleTextPacket(MinecraftByteBuf buf) {
        this.text = buf.readComponent();
    }

    @Override
    public void serialize(MinecraftByteBuf buf) {
        buf.writeComponent(this.text);
    }
}
