package com.github.steveice10.mc.protocol.data.game.entity.metadata;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

@Data
@AllArgsConstructor
public abstract class EntityMetadata<V, T extends MetadataType<V>> {
    protected final int id;
    protected final @NotNull T type;

    /**
     * May be null depending on type
     *
     * @return The value of this metadata.
     */
    public abstract V getValue();

    /**
     * Overridden for primitive classes. This write method still checks for these primitives in the event
     * they are manually created using {@link ObjectEntityMetadata}.
     *
     * @param helper The codec helper.
     * @param out    The output buffer.
     */
    public void write(MinecraftCodecHelper helper, ByteBuf out) throws IOException {
        this.type.writeMetadata(helper, out, this.getValue());
    }

    @Override
    public String toString() {
        return "EntityMetadata(id=" + id + ", type=" + type + ", value=" + getValue().toString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityMetadata<?, ?> that)) {
            return false;
        }
        return this.id == that.id && this.type == that.type && Objects.equals(this.getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, getValue());
    }
}
