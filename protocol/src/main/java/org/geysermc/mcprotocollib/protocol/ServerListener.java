package org.geysermc.mcprotocollib.protocol;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.compression.CompressionConfig;
import org.geysermc.mcprotocollib.network.compression.ZlibCompression;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.data.status.PlayerInfo;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.VersionInfo;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoBuilder;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundKeyPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ping.clientbound.ClientboundPongResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.ping.serverbound.ServerboundPingRequestPacket;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Handles initial login and status requests for servers.
 */
public class ServerListener extends SessionAdapter {
    private static final int DEFAULT_COMPRESSION_THRESHOLD = 256;

    // Always empty post-1.7
    private static final String SERVER_ID = "";
    private static final KeyPair KEY_PAIR;

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024);
            KEY_PAIR = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate server key pair.", e);
        }
    }

    private final NbtMap networkCodec;

    private final byte[] challenge = new byte[4];
    private String username = "";
    private KeepAliveState keepAliveState;

    @Getter
    private boolean isTransfer = false;

    public ServerListener(NbtMap networkCodec) {
        this.networkCodec = networkCodec;
        new Random().nextBytes(this.challenge);
    }

    @Override
    public void connected(ConnectedEvent event) {
        Session session = event.getSession();
        session.setFlag(MinecraftConstants.PING_KEY, 0L);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        MinecraftProtocol protocol = session.getPacketProtocol();
        if (protocol.getInboundState() == ProtocolState.HANDSHAKE) {
            if (packet instanceof ClientIntentionPacket intentionPacket) {
                switch (intentionPacket.getIntent()) {
                    case STATUS -> {
                        session.switchOutboundState(ProtocolState.STATUS);
                        session.switchInboundState(ProtocolState.STATUS);
                    }
                    case TRANSFER -> beginLogin(session, protocol, intentionPacket, true);
                    case LOGIN -> beginLogin(session, protocol, intentionPacket, false);
                    default -> throw new UnsupportedOperationException("Invalid client intent: " + intentionPacket.getIntent());
                }
            }
        } else if (protocol.getInboundState() == ProtocolState.LOGIN) {
            if (packet instanceof ServerboundHelloPacket helloPacket) {
                this.username = helloPacket.getUsername();

                if (session.getFlag(MinecraftConstants.ENCRYPT_CONNECTION, true)) {
                    session.send(new ClientboundHelloPacket(SERVER_ID, KEY_PAIR.getPublic(), this.challenge, session.getFlag(MinecraftConstants.SHOULD_AUTHENTICATE, true)));
                } else {
                    new Thread(() -> authenticate(session, false, null)).start();
                }
            } else if (packet instanceof ServerboundKeyPacket keyPacket) {
                PrivateKey privateKey = KEY_PAIR.getPrivate();

                if (!Arrays.equals(this.challenge, keyPacket.getEncryptedChallenge(privateKey))) {
                    throw new IllegalStateException("Protocol error");
                }

                SecretKey key = keyPacket.getSecretKey(privateKey);
                session.setEncryption(protocol.createEncryption(key));
                new Thread(() -> authenticate(session, session.getFlag(MinecraftConstants.SHOULD_AUTHENTICATE, true), key)).start();
            } else if (packet instanceof ServerboundLoginAcknowledgedPacket) {
                session.switchOutboundState(ProtocolState.CONFIGURATION);
                session.switchInboundState(ProtocolState.CONFIGURATION);
                keepAliveState = new KeepAliveState();
                if (session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
                    // If keepalive state is null, lets assume there is no keepalive thread yet
                    new Thread(() -> keepAlive(session)).start();
                }

                // Credit ViaVersion: https://github.com/ViaVersion/ViaVersion/blob/dev/common/src/main/java/com/viaversion/viaversion/protocols/protocol1_20_5to1_20_3/rewriter/EntityPacketRewriter1_20_5.java
                for (Map.Entry<String, Object> entry : networkCodec.entrySet()) {
                    @SuppressWarnings("PatternValidation")
                    NbtMap entryTag = (NbtMap) entry.getValue();
                    @SuppressWarnings("PatternValidation")
                    Key typeTag = Key.key(entryTag.getString("type"));
                    List<NbtMap> valueTag = entryTag.getList("value", NbtType.COMPOUND);
                    List<RegistryEntry> entries = new ArrayList<>();
                    for (NbtMap compoundTag : valueTag) {
                        @SuppressWarnings("PatternValidation")
                        Key nameTag = Key.key(compoundTag.getString("name"));
                        int id = compoundTag.getInt("id");
                        entries.add(id, new RegistryEntry(nameTag, compoundTag.getCompound("element")));
                    }

                    session.send(new ClientboundRegistryDataPacket(typeTag, entries));
                }

                session.send(new ClientboundFinishConfigurationPacket());
            }
        } else if (protocol.getInboundState() == ProtocolState.STATUS) {
            if (packet instanceof ServerboundStatusRequestPacket) {
                ServerInfoBuilder builder = session.getFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY);
                if (builder == null) {
                    builder = $ -> new ServerStatusInfo(
                        Component.text("A Minecraft Server"),
                        new PlayerInfo(0, 20, new ArrayList<>()),
                        new VersionInfo(protocol.getCodec().getMinecraftVersion(), protocol.getCodec().getProtocolVersion()),
                        null,
                        false
                    );
                }

                ServerStatusInfo info = builder.buildInfo(session);
                session.send(new ClientboundStatusResponsePacket(info));
            } else if (packet instanceof ServerboundPingRequestPacket pingRequestPacket) {
                session.send(new ClientboundPongResponsePacket(pingRequestPacket.getPingTime()));
            }
        } else if (protocol.getInboundState() == ProtocolState.GAME) {
            if (packet instanceof ServerboundKeepAlivePacket keepAlivePacket) {
                handleKeepAlive(session, keepAlivePacket);
            } else if (packet instanceof ServerboundConfigurationAcknowledgedPacket) {
                // The developer who sends ClientboundStartConfigurationPacket needs to setOutboundState to CONFIGURATION
                // after sending the packet. We can't do it in this class because it needs to be a method call right after it was sent.
                // Using nettys event loop to change outgoing state may cause differences to vanilla.
                session.switchInboundState(ProtocolState.CONFIGURATION);
                keepAliveState = new KeepAliveState();
            } else if (packet instanceof ServerboundPingRequestPacket pingRequestPacket) {
                session.send(new ClientboundPongResponsePacket(pingRequestPacket.getPingTime()));
                session.disconnect(Component.translatable("multiplayer.status.request_handled"));
            }
        } else if (protocol.getInboundState() == ProtocolState.CONFIGURATION) {
            if (packet instanceof ServerboundKeepAlivePacket keepAlivePacket) {
                handleKeepAlive(session, keepAlivePacket);
            } else if (packet instanceof ServerboundFinishConfigurationPacket) {
                session.switchOutboundState(ProtocolState.GAME);
                session.switchInboundState(ProtocolState.GAME);
                keepAliveState = new KeepAliveState();
                ServerLoginHandler handler = session.getFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY);
                if (handler != null) {
                    handler.loggedIn(session);
                }
            }
        }
    }

    private void handleKeepAlive(Session session, ServerboundKeepAlivePacket keepAlivePacket) {
        KeepAliveState currentKeepAliveState = this.keepAliveState;
        if (currentKeepAliveState != null) {
            if (currentKeepAliveState.keepAlivePending && keepAlivePacket.getPingId() == currentKeepAliveState.keepAliveChallenge) {
                currentKeepAliveState.keepAlivePending = false;
                session.setFlag(MinecraftConstants.PING_KEY, System.currentTimeMillis() - currentKeepAliveState.keepAliveTime);
            } else {
                session.disconnect(Component.translatable("disconnect.timeout"));
            }
        }
    }

    private void beginLogin(Session session, MinecraftProtocol protocol, ClientIntentionPacket packet, boolean transferred) {
        isTransfer = transferred;
        session.switchOutboundState(ProtocolState.LOGIN);
        if (transferred && !session.getFlag(MinecraftConstants.ACCEPT_TRANSFERS_KEY)) {
            session.disconnect(Component.translatable("multiplayer.disconnect.transfers_disabled"));
        } else if (packet.getProtocolVersion() > protocol.getCodec().getProtocolVersion()) {
            session.disconnect(Component.translatable("multiplayer.disconnect.incompatible", Component.text(protocol.getCodec().getMinecraftVersion())));
        } else if (packet.getProtocolVersion() < protocol.getCodec().getProtocolVersion()) {
            session.disconnect(Component.translatable("multiplayer.disconnect.outdated_client", Component.text(protocol.getCodec().getMinecraftVersion())));
        } else {
            session.switchInboundState(ProtocolState.LOGIN);
        }
    }

    private void authenticate(Session session, boolean shouldAuthenticate, SecretKey key) {
        GameProfile profile;
        if (shouldAuthenticate && key != null) {
            SessionService sessionService = session.getFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());
            try {
                profile = sessionService.getProfileByServer(username, SessionService.getServerId(SERVER_ID, KEY_PAIR.getPublic(), key));
            } catch (IOException e) {
                session.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"), e);
                return;
            }

            if (profile == null) {
                session.disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                return;
            }
        } else {
            profile = new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()), username);
        }

        session.setFlag(MinecraftConstants.PROFILE_KEY, profile);

        int threshold = session.getFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, DEFAULT_COMPRESSION_THRESHOLD);
        if (threshold >= 0) {
            session.send(new ClientboundLoginCompressionPacket(threshold), () ->
                session.setCompression(new CompressionConfig(threshold, new ZlibCompression(), true)));
        }

        session.send(new ClientboundLoginFinishedPacket(profile));
    }

    private void keepAlive(Session session) {
        while (session.isConnected()) {
            KeepAliveState currentKeepAliveState = this.keepAliveState;
            if (currentKeepAliveState != null) {
                if (System.currentTimeMillis() - currentKeepAliveState.keepAliveTime >= 15000L) {
                    if (currentKeepAliveState.keepAlivePending) {
                        session.disconnect(Component.translatable("disconnect.timeout"));
                        break;
                    }

                    long time = System.currentTimeMillis();

                    currentKeepAliveState.keepAlivePending = true;
                    currentKeepAliveState.keepAliveChallenge = time;
                    currentKeepAliveState.keepAliveTime = time;
                    session.send(new ClientboundKeepAlivePacket(currentKeepAliveState.keepAliveChallenge));
                }
            }

            // TODO: Implement proper tick loop rather than sleeping
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static class KeepAliveState {
        private boolean keepAlivePending;
        private long keepAliveChallenge;
        private long keepAliveTime = System.currentTimeMillis();
    }
}
