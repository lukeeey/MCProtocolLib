package org.geysermc.mcprotocollib.network.event.session;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
/**
 * Called when a session encounters an error while reading or writing packet data.
 */
public class PacketErrorEvent implements SessionEvent {
    private final Session session;
    private final Throwable cause;
    private final @Nullable Class<? extends Packet> packetClass;
    private boolean suppress = false;

    /**
     * Creates a new SessionErrorEvent instance.
     *
     * @param session Session that the error came from.
     * @param cause Cause of the error.
     */
    public PacketErrorEvent(Session session, Throwable cause, @Nullable Packet packetClass) {
        this.session = session;
        this.cause = cause;
        this.packetClass = packetClass == null ? null : packetClass.getClass();
    }

    /**
     * Gets the session involved in this event.
     *
     * @return The event's session.
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * Gets the Throwable responsible for the error.
     *
     * @return The Throwable responsible for the error.
     */
    public Throwable getCause() {
        return this.cause;
    }

    /**
     * Gets the packet class where the error occurred in, if it is known.
     *
     * @return the packet class related to this packet error
     */
    public Class<? extends Packet> getPacketClass() {
        return this.packetClass;
    }

    /**
     * Gets whether the error should be suppressed. If the error is not suppressed,
     * it will be passed on through internal error handling and disconnect the session.
     * The default value is false.
     *
     * @return Whether the error should be suppressed.
     */
    public boolean shouldSuppress() {
        return this.suppress;
    }

    /**
     * Sets whether the error should be suppressed. If the error is not suppressed,
     * it will be passed on through internal error handling and disconnect the session.
     *
     * @param suppress Whether the error should be suppressed.
     */
    public void setSuppress(boolean suppress) {
        this.suppress = suppress;
    }

    @Override
    public void call(SessionListener listener) {
        listener.packetError(this);
    }
}
