package org.geysermc.mcprotocollib.protocol.example;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.StepMCToken;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.Server;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.server.ServerAdapter;
import org.geysermc.mcprotocollib.network.event.server.ServerClosedEvent;
import org.geysermc.mcprotocollib.network.event.server.SessionAddedEvent;
import org.geysermc.mcprotocollib.network.event.server.SessionRemovedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.server.NetworkServer;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.status.PlayerInfo;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.VersionInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;

public class MinecraftProtocolTest {
    private static final Logger log = LoggerFactory.getLogger(MinecraftProtocolTest.class);
    private static final boolean SPAWN_SERVER = true;
    private static final boolean ENCRYPT_CONNECTION = true;
    private static final boolean SHOULD_AUTHENTICATE = false;
    private static final SocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 25565);
    private static final ProxyInfo PROXY = null;
    private static final ProxyInfo AUTH_PROXY = null;
    private static final String USERNAME = "Username";
    private static final String PASSWORD = "Password";

    public static void main(String[] args) {
        if (SPAWN_SERVER) {
            SessionService sessionService = new SessionService();
            sessionService.setProxy(AUTH_PROXY);

            Server server = new NetworkServer(ADDRESS, MinecraftProtocol::new);
            server.setGlobalFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
            server.setGlobalFlag(MinecraftConstants.ENCRYPT_CONNECTION, ENCRYPT_CONNECTION);
            server.setGlobalFlag(MinecraftConstants.SHOULD_AUTHENTICATE, SHOULD_AUTHENTICATE);
            server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, session ->
                    new ServerStatusInfo(
                            Component.text("Hello world!"),
                            new PlayerInfo(100, 0, new ArrayList<>()),
                            new VersionInfo(MinecraftCodec.CODEC.getMinecraftVersion(), MinecraftCodec.CODEC.getProtocolVersion()),
                            null,
                            false
                    )
            );

            server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, session ->
                    session.send(new ClientboundLoginPacket(
                            0,
                            false,
                            new Key[]{Key.key("minecraft:world")},
                            0,
                            16,
                            16,
                            false,
                            false,
                            false,
                            new PlayerSpawnInfo(
                                    0,
                                    Key.key("minecraft:world"),
                                    100,
                                    GameMode.SURVIVAL,
                                    GameMode.SURVIVAL,
                                    false,
                                    false,
                                    null,
                                    100,
                                    5
                            ),
                            true
                    ))
            );

            server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256);
            server.addListener(new ServerAdapter() {
                @Override
                public void serverClosed(ServerClosedEvent event) {
                    log.info("Server closed.");
                }

                @Override
                public void sessionAdded(SessionAddedEvent event) {
                    event.getSession().addListener(new SessionAdapter() {
                        @Override
                        public void packetReceived(Session session, Packet packet) {
                            if (packet instanceof ServerboundChatPacket chatPacket) {
                                GameProfile profile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);
                                log.info("{}: {}", profile.getName(), chatPacket.getMessage());

                                Component msg = Component.text("Hello, ")
                                        .color(NamedTextColor.GREEN)
                                        .append(Component.text(profile.getName())
                                                .color(NamedTextColor.AQUA)
                                                .decorate(TextDecoration.UNDERLINED))
                                        .append(Component.text("!")
                                                .color(NamedTextColor.GREEN));

                                session.send(new ClientboundSystemChatPacket(msg, false));
                            }
                        }
                    });
                }

                @Override
                public void sessionRemoved(SessionRemovedEvent event) {
                    MinecraftProtocol protocol = (MinecraftProtocol) event.getSession().getPacketProtocol();
                    if (protocol.getOutboundState() == ProtocolState.GAME) {
                        log.info("Closing server.");
                        event.getServer().close(false);
                    }
                }
            });

            server.bind();
        }

        status();
        login();
    }

    private static void status() {
        SessionService sessionService = new SessionService();
        sessionService.setProxy(AUTH_PROXY);

        MinecraftProtocol protocol = new MinecraftProtocol();
        ClientSession client = ClientNetworkSessionFactory.factory()
            .setRemoteSocketAddress(ADDRESS)
            .setProtocol(protocol)
            .setProxy(PROXY)
            .create();
        client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
        client.setFlag(MinecraftConstants.SERVER_INFO_HANDLER_KEY, (session, info) -> {
            log.info("Version: {}, {}", info.getVersionInfo().getVersionName(), info.getVersionInfo().getProtocolVersion());
            log.info("Player Count: {} / {}", info.getPlayerInfo().getOnlinePlayers(), info.getPlayerInfo().getMaxPlayers());
            log.info("Players: {}", Arrays.toString(info.getPlayerInfo().getPlayers().toArray()));
            log.info("Description: {}", info.getDescription());
            log.info("Icon: {}", new String(Base64.getEncoder().encode(info.getIconPng()), StandardCharsets.UTF_8));
        });

        client.setFlag(MinecraftConstants.SERVER_PING_TIME_HANDLER_KEY, (session, pingTime) ->
                log.info("Server ping took {}ms", pingTime));

        client.connect();
        while (client.isConnected()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for server to disconnect.", e);
            }
        }
    }

    private static void login() {
        MinecraftProtocol protocol;
        if (SHOULD_AUTHENTICATE) {
            StepFullJavaSession.FullJavaSession fullJavaSession;
            try {
                fullJavaSession = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(
                        MinecraftAuth.createHttpClient(),
                        new StepCredentialsMsaCode.MsaCredentials(USERNAME, PASSWORD));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            StepMCProfile.MCProfile mcProfile = fullJavaSession.getMcProfile();
            StepMCToken.MCToken mcToken = mcProfile.getMcToken();
            protocol = new MinecraftProtocol(
                    new GameProfile(mcProfile.getId(), mcProfile.getName()),
                    mcToken.getAccessToken());
            log.info("Successfully authenticated user.");
        } else {
            protocol = new MinecraftProtocol(USERNAME);
        }

        SessionService sessionService = new SessionService();
        sessionService.setProxy(AUTH_PROXY);


        ClientSession client = ClientNetworkSessionFactory.factory()
            .setRemoteSocketAddress(ADDRESS)
            .setProtocol(protocol)
            .setProxy(PROXY)
            .create();
        client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
        client.addListener(new SessionAdapter() {
            @Override
            public void packetReceived(Session session, Packet packet) {
                if (packet instanceof ClientboundLoginPacket) {
                    session.send(new ServerboundChatPacket("Hello, this is a test of MCProtocolLib.", Instant.now().toEpochMilli(), 0L, null, 0, new BitSet()));
                } else if (packet instanceof ClientboundSystemChatPacket systemChatPacket) {
                    Component message = systemChatPacket.getContent();
                    log.info("Received Message: {}", message);
                    session.disconnect(Component.text("Finished"));
                }
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                log.info("Disconnected: {}", event.getDetails().reason(), event.getDetails().cause());
            }
        });

        client.connect();
    }
}
