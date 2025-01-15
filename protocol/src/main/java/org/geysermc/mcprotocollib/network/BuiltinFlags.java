package org.geysermc.mcprotocollib.network;

import java.net.InetSocketAddress;

/**
 * Built-in PacketLib session flags.
 */
public class BuiltinFlags {

    /**
     * Enables HAProxy protocol support.
     * When this value is not null it represents the ip and port the client claims the connection is from.
     */
    public static final Flag<InetSocketAddress> CLIENT_PROXIED_ADDRESS = new Flag<>("client-proxied-address", InetSocketAddress.class);

    /**
     * Whether the current client is transferring.
     */
    public static final Flag<Boolean> CLIENT_TRANSFERRING = new Flag<>("transferring", Boolean.class);

    /**
     * When set to false, an SRV record resolve is not attempted.
     */
    public static final Flag<Boolean> ATTEMPT_SRV_RESOLVE = new Flag<>("attempt-srv-resolve", Boolean.class);

    /**
     * When set to true, the client or server will attempt to use TCP Fast Open if supported.
     */
    public static final Flag<Boolean> TCP_FAST_OPEN = new Flag<>("tcp-fast-open", Boolean.class);

    /**
     * Connection timeout in seconds.
     * Only used by the client.
     */
    public static final Flag<Integer> CLIENT_CONNECT_TIMEOUT = new Flag<>("client-connect-timeout", Integer.class);
    
    /**
     * Read timeout in seconds.
     * Used by both the server and client.
     */
    public static final Flag<Integer> READ_TIMEOUT = new Flag<>("read-timeout", Integer.class);

    /**
     * Write timeout in seconds.
     * Used by both the server and client.
     */
    public static final Flag<Integer> WRITE_TIMEOUT = new Flag<>("write-timeout", Integer.class);

    private BuiltinFlags() {
    }
}
