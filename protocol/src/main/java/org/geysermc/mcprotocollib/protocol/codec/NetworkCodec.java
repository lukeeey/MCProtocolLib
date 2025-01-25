package org.geysermc.mcprotocollib.protocol.codec;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record NetworkCodec<T>(Writer<T> writer, Reader<T> reader) {
    public static <T> NetworkCodec<T> of(Writer<T> writer, Reader<T> reader) {
        return new NetworkCodec<>(writer, reader);
    }

    public <O> NetworkCodec<O> map(Function<O, T> to, Function<T, O> from) {
        return NetworkCodec.of(
            (buf, o) -> writer.write(buf, to.apply(o)),
            buf -> from.apply(reader.read(buf))
        );
    }

    public NetworkCodec<Optional<T>> optional() {
        return NetworkCodec.of(
            (buf, optional) -> {
                MinecraftTypes.BOOLEAN.write(buf, optional.isPresent());
                optional.ifPresent(t -> this.write(buf, t));
            },
            buf -> MinecraftTypes.BOOLEAN.read(buf) ? Optional.of(this.read(buf)) : Optional.empty()
        );
    }

    public NetworkCodec<T> nullable() {
        return optional().map(Optional::ofNullable, optional -> optional.orElse(null));
    }

    public NetworkCodec<List<T>> list() {
        return list(Integer.MAX_VALUE);
    }

    public NetworkCodec<List<T>> list(int maxSize) {
        return new NetworkCodec<>(
            (buf, list) -> {
                int size = list.size();
                if (size > maxSize) {
                    throw new IllegalArgumentException("List size " + size + " is greater than maximum allowed size " + maxSize);
                }

                MinecraftTypes.writeVarInt(buf, list.size());
                for (T t : list) {
                    write(buf, t);
                }
            },
            buf -> {
                int size = MinecraftTypes.readVarInt(buf);
                if (size > maxSize) {
                    throw new IllegalArgumentException("List size " + size + " is greater than maximum allowed size " + maxSize);
                }

                List<T> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(read(buf));
                }
                return list;
            }
        );
    }

    public void write(ByteBuf byteBuf, T t) {
        writer.write(byteBuf, t);
    }

    public T read(ByteBuf byteBuf) {
        return reader.read(byteBuf);
    }

    public interface Writer<T> {
        void write(ByteBuf buf, T t);
    }

    public interface Reader<T> {
        T read(ByteBuf buf);
    }
}
