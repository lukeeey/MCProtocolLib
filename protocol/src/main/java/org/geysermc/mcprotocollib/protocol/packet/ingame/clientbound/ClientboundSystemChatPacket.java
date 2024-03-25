package org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound;

import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;
import net.kyori.adventure.text.Component;

@Data
@With
@AllArgsConstructor
public class ClientboundSystemChatPacket implements MinecraftPacket {
    private final Component content;
    private final boolean overlay;

    public ClientboundSystemChatPacket(ByteBuf in, MinecraftCodecHelper helper) {
        this.content = helper.readComponent(in);
        this.overlay = in.readBoolean();
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
        helper.writeComponent(out, this.content);
        out.writeBoolean(this.overlay);
    }
}