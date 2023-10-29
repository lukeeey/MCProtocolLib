package com.github.steveice10.mc.protocol.packet.common.clientbound;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@With
@AllArgsConstructor
public class ClientboundResourcePackPacket implements MinecraftPacket {
    private final @NotNull String url;
    private final @NotNull String hash;
    private final boolean required;
    private final @Nullable Component prompt;

    public ClientboundResourcePackPacket(ByteBuf in, MinecraftCodecHelper helper) {
        this.url = helper.readString(in);
        this.hash = helper.readString(in);
        this.required = in.readBoolean();
        if (in.readBoolean()) {
            this.prompt = helper.readComponent(in);
        } else {
            this.prompt = null;
        }
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
        helper.writeString(out, this.url);
        helper.writeString(out, this.hash);
        out.writeBoolean(this.required);
        out.writeBoolean(this.prompt != null);
        if (this.prompt != null) {
            helper.writeComponent(out, this.prompt);
        }
    }
}
