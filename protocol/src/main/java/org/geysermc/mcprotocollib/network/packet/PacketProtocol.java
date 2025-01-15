package org.geysermc.mcprotocollib.network.packet;

import io.netty.buffer.ByteBuf;
import org.geysermc.mcprotocollib.network.Server;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.codec.PacketCodecHelper;

/**
 * A protocol for packet sending and receiving.
 * All implementations must have a constructor that takes in a {@link ByteBuf}.
 */
public abstract class PacketProtocol {
    /**
     * Gets the prefix used when locating SRV records for this protocol.
     *
     * @return The protocol's SRV record prefix.
     */
    public abstract String getSRVRecordPrefix();

    /**
     * Gets the packet header of this protocol.
     *
     * @return The protocol's packet header.
     */
    public abstract PacketHeader getPacketHeader();

    /**
     * Creates a new {@link PacketCodecHelper} that can be used
     * for each session.
     *
     * @return A new {@link PacketCodecHelper}.
     */
    public abstract PacketCodecHelper createHelper();

    /**
     * Called when a client session is created with this protocol.
     *
     * @param session The created session.
     */
    public abstract void newClientSession(Session session);

    /**
     * Called when a server session is created with this protocol.
     *
     * @param server The server that the session belongs to.
     * @param session The created session.
     */
    public abstract void newServerSession(Server server, Session session);

    /**
     * Gets the inbound packet registry for this protocol.
     *
     * @return The protocol's inbound packet registry.
     */
    public abstract PacketRegistry getInboundPacketRegistry();

    /**
     * Gets the outbound packet registry for this protocol.
     *
     * @return The protocol's outbound packet registry.
     */
    public abstract PacketRegistry getOutboundPacketRegistry();
}
