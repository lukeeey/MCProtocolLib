package org.geysermc.mcprotocollib.network.event.session;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.helper.DisconnectionDetails;

/**
 * Called when the session is about to disconnect.
 */
public class DisconnectingEvent implements SessionEvent {
    private final Session session;
    private final DisconnectionDetails disconnectionDetails;

    /**
     * Creates a new DisconnectingEvent instance.
     *
     * @param session Session being disconnected.
     * @param disconnectionDetails Reason for the disconnection.
     */
    public DisconnectingEvent(Session session, DisconnectionDetails disconnectionDetails) {
        this.session = session;
        this.disconnectionDetails = disconnectionDetails;
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
     * Gets the reason given for the session disconnecting.
     *
     * @return The event's reason.
     */
    public DisconnectionDetails getDetails() {
        return this.disconnectionDetails;
    }

    @Override
    public void call(SessionListener listener) {
        listener.disconnecting(this);
    }
}
