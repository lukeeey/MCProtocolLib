package org.geysermc.mcprotocollib.protocol;

import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.Flag;
import org.geysermc.mcprotocollib.network.packet.DefaultPacketHeader;
import org.geysermc.mcprotocollib.network.packet.PacketHeader;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoBuilder;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoHandler;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerPingTimeHandler;

/**
 * Class containing various constants for Minecraft sessions.
 */
public final class MinecraftConstants {
    // General Key Constants

    /**
     * Session flag where the user {@link GameProfile} is stored.
     */
    public static final Flag<GameProfile> PROFILE_KEY = new Flag<>("profile", GameProfile.class);

    /**
     * Session flag for providing a custom {@link SessionService} instance.
     */
    public static final Flag<SessionService> SESSION_SERVICE_KEY = new Flag<>("session-service", SessionService.class);

    /**
     * Session flag for whether to automatically send a keep alive packet. <br>
     * For servers: determines if the keep alive task is run. <br>
     * For clients: determines if MCProtocolLib automatically sends back a packet
     * when the server sends their keep alive packet.
     */
    public static final Flag<Boolean> AUTOMATIC_KEEP_ALIVE_MANAGEMENT = new Flag<>("manage-keep-alive", Boolean.class);

    // Client Key Constants

    /**
     * Session flag where the user's access token is stored. Client only.
     */
    public static final Flag<String> ACCESS_TOKEN_KEY = new Flag<>("access-token", String.class);

    /**
     * Session flag for providing a custom server info response handler. Client only.
     */
    public static final Flag<ServerInfoHandler> SERVER_INFO_HANDLER_KEY = new Flag<>("server-info-handler", ServerInfoHandler.class);

    /**
     * Session flag for providing a custom ping time response handler. Client only.
     */
    public static final Flag<ServerPingTimeHandler> SERVER_PING_TIME_HANDLER_KEY = new Flag<>("server-ping-time-handler", ServerPingTimeHandler.class);

    /**
     * Session flag for determining if an automatic response should be sent to the ClientboundSelectKnownPacks.
     */
    public static final Flag<Boolean> SEND_BLANK_KNOWN_PACKS_RESPONSE = new Flag<>("send-blank-known-packs-response", Boolean.class);

    /**
     * Session flag for determining whether to create a new TcpClientSession when the Java server sends a ClientboundTransferPacket.
     */
    public static final Flag<Boolean> FOLLOW_TRANSFERS = new Flag<>("follow-transfers", Boolean.class);

    /**
     * Session flag where the host address is stored. Client only.
     */
    public static final Flag<String> CLIENT_HOST = new Flag<>("client-host", String.class);

    /**
     * Session flag where the port is stored. Client only.
     */
    public static final Flag<Integer> CLIENT_PORT = new Flag<>("client-port", Integer.class);

    // Server Key Constants

    /**
     * Session flag for determining whether to encrypt the connection. Server only.
     */
    public static final Flag<Boolean> ENCRYPT_CONNECTION = new Flag<>("encrypt-connection", Boolean.class);

    /**
     * Session flag for determining whether to authenticate users with the session service. Server only.
     */
    public static final Flag<Boolean> SHOULD_AUTHENTICATE = new Flag<>("should-authenticate", Boolean.class);

    /**
     * Session flag for determining whether to accept transferred connections. Server only.
     */
    public static final Flag<Boolean> ACCEPT_TRANSFERS_KEY = new Flag<>("accept-transfers", Boolean.class);

    /**
     * Session flag for providing a custom server info response builder. Server only.
     */
    public static final Flag<ServerInfoBuilder> SERVER_INFO_BUILDER_KEY = new Flag<>("info-builder", ServerInfoBuilder.class);

    /**
     * Session flag for providing a custom server login handler. Server only.
     */
    public static final Flag<ServerLoginHandler> SERVER_LOGIN_HANDLER_KEY = new Flag<>("login-handler", ServerLoginHandler.class);

    /**
     * Session flag for storing the current ping time. Server only.
     */
    public static final Flag<Long> PING_KEY = new Flag<>("ping", Long.class);

    /**
     * Session flag for determining the packet compression threshold. Server only.
     */
    public static final Flag<Integer> SERVER_COMPRESSION_THRESHOLD = new Flag<>("compression-threshold", Integer.class);

    /**
     * The packet header used by Minecraft.
     */
    public static final PacketHeader PACKET_HEADER = new DefaultPacketHeader();

    /**
     * The SRV Record prefix used by Minecraft.
     */
    public static final String SRV_RECORD_PREFIX = "_minecraft";

    private MinecraftConstants() {
    }
}
