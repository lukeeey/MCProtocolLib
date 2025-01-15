package org.geysermc.mcprotocollib.protocol;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Server;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.server.NetworkServer;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.status.PlayerInfo;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.VersionInfo;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

public class MinecraftProtocolTest {
    private static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 25562);

    private static final ServerStatusInfo SERVER_INFO = new ServerStatusInfo(
            Component.text("Hello world!"),
            new PlayerInfo(100, 0, new ArrayList<>()),
            new VersionInfo(MinecraftCodec.CODEC.getMinecraftVersion(), MinecraftCodec.CODEC.getProtocolVersion()),
            null,
            false
    );
    private static final ClientboundLoginPacket JOIN_GAME_PACKET = new ClientboundLoginPacket(0, false, new Key[]{Key.key("minecraft:world")}, 0, 16, 16, false, false, false, new PlayerSpawnInfo(0, Key.key("minecraft:world"), 100, GameMode.SURVIVAL, GameMode.SURVIVAL, false, false, null, 100, 20), true);
    private static final Logger log = LoggerFactory.getLogger(MinecraftProtocolTest.class);

    private static Server server;

    @BeforeAll
    public static void setupServer() {
        server = new NetworkServer(ADDRESS, MinecraftProtocol::new);
        server.setGlobalFlag(MinecraftConstants.ENCRYPT_CONNECTION, true);
        server.setGlobalFlag(MinecraftConstants.SHOULD_AUTHENTICATE, false);
        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, session -> SERVER_INFO);
        server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, session -> {
            // Seems like in this setup the server can reply too quickly to ServerboundFinishConfigurationPacket
            // before the client can transition CONFIGURATION -> GAME. There is probably something wrong here and this is just a band-aid.
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Failed to wait to send ClientboundLoginPacket: {}", e.getMessage());
            }
            session.send(JOIN_GAME_PACKET);
        });

        assertTrue(server.bind(true).isListening(), "Could not bind server.");
    }

    @AfterAll
    public static void tearDownServer() {
        if (server != null) {
            server.close(true);
            server = null;
        }
    }

    @Test
    public void testStatus() throws InterruptedException {
        ClientSession session = ClientNetworkSessionFactory.factory()
            .setRemoteSocketAddress(ADDRESS)
            .setProtocol(new MinecraftProtocol())
            .create();
        try {
            ServerInfoHandlerTest handler = new ServerInfoHandlerTest();
            session.setFlag(MinecraftConstants.SERVER_INFO_HANDLER_KEY, handler);
            session.addListener(new DisconnectListener());
            session.connect();

            assertTrue(handler.status.await(4, SECONDS), "Did not receive server info in time.");
            assertNotNull(handler.info, "Failed to get server info.");
            assertEquals(SERVER_INFO, handler.info, "Received incorrect server info.");
        } finally {
            session.disconnect(Component.text("Status test complete."));
        }
    }

    @Test
    public void testLogin() throws InterruptedException {
        ClientSession session = ClientNetworkSessionFactory.factory()
            .setRemoteSocketAddress(ADDRESS)
            .setProtocol(new MinecraftProtocol("Username"))
            .create();
        try {
            LoginListenerTest listener = new LoginListenerTest();
            session.addListener(listener);
            session.addListener(new DisconnectListener());
            session.connect();

            assertTrue(listener.login.await(4, SECONDS), "Did not receive login packet in time.");
            assertNotNull(listener.packet, "Failed to log in.");
            assertEquals(JOIN_GAME_PACKET, listener.packet, "Received incorrect join packet.");
        } finally {
            session.disconnect(Component.text("Login test complete."));
        }
    }

    private static class ServerInfoHandlerTest implements ServerInfoHandler {
        public final CountDownLatch status = new CountDownLatch(1);
        public ServerStatusInfo info;

        @Override
        public void handle(Session session, ServerStatusInfo info) {
            this.info = info;
            this.status.countDown();
        }
    }

    private static class LoginListenerTest extends SessionAdapter {
        public final CountDownLatch login = new CountDownLatch(1);
        public ClientboundLoginPacket packet;

        @Override
        public void packetReceived(Session session, Packet packet) {
            if (packet instanceof ClientboundLoginPacket loginPacket) {
                this.packet = loginPacket;
                this.login.countDown();
            }
        }
    }

    private static class DisconnectListener extends SessionAdapter {
        @Override
        public void disconnected(DisconnectedEvent event) {
            log.error("Disconnected: {}", event.getReason(), event.getCause());
        }
    }
}
