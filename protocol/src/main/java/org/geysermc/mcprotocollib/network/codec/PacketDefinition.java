package org.geysermc.mcprotocollib.network.codec;

import io.netty.buffer.ByteBuf;
import org.geysermc.mcprotocollib.network.packet.Packet;

/**
 * Represents a definition of a packet with various
 * information about it, such as it's id, class and
 * factory for construction.
 *
 * @param <T> the packet type
 */
public class PacketDefinition<T extends Packet> {
    private final int id;
    private final Class<T> packetClass;
    private final PacketSerializer<T> serializer;

    public PacketDefinition(final int id, final Class<T> packetClass, final PacketSerializer<T> serializer) {
        this.id = id;
        this.packetClass = packetClass;
        this.serializer = serializer;
    }

    /**
     * Returns the id of the packet.
     *
     * @return the id of the packet
     */
    public int getId() {
        return this.id;
    }

    /**
     * Returns the class of the packet.
     *
     * @return the class of the packet
     */
    public Class<T> getPacketClass() {
        return this.packetClass;
    }

    /**
     * Returns the {@link PacketSerializer} of the packet.
     *
     * @return the packet serializer of the packet
     */
    public PacketSerializer<T> getSerializer() {
        return this.serializer;
    }

    public T newInstance(ByteBuf buf) {
        return this.serializer.deserialize(buf, this);
    }
}
