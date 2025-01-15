package org.geysermc.mcprotocollib.network;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A network client session.
 */
public interface ClientSession extends Session {
    /**
     * Connects this session to its host and port.
     */
    default void connect() {
        connect(true);
    }

    /**
     * Connects this session to its host and port.
     *
     * @param wait Whether to wait for the connection to be established before returning.
     */
    void connect(boolean wait);

    /**
     * Get the proxy used by this session.
     *
     * @return The proxy used by this session.
     */
    @Nullable ProxyInfo getProxy();
}
