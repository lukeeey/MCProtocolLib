package org.geysermc.mcprotocollib.protocol;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.compression.CompressionConfig;
import org.geysermc.mcprotocollib.network.compression.ZlibCompression;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.session.ClientNetworkSession;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.UnexpectedEncryptionException;
import org.geysermc.mcprotocollib.protocol.data.handshake.HandshakeIntent;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerInfoHandler;
import org.geysermc.mcprotocollib.protocol.data.status.handler.ServerPingTimeHandler;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundTransferPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundKeyPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ping.clientbound.ClientboundPongResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.ping.serverbound.ServerboundPingRequestPacket;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Objects;

/**
 * Handles making initial login and status requests for clients.
 */
@AllArgsConstructor
public class ClientListener extends SessionAdapter {
    private final @NonNull HandshakeIntent handshakeIntent;

    @SneakyThrows
    @Override
    public void packetReceived(Session session, Packet packet) {
        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (protocol.getInboundState() == ProtocolState.LOGIN) {
            if (packet instanceof ClientboundHelloPacket helloPacket) {
                GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                String accessToken = session.getFlag(MinecraftConstants.ACCESS_TOKEN_KEY);

                if ((profile == null || accessToken == null) && helloPacket.isShouldAuthenticate()) {
                    throw new UnexpectedEncryptionException();
                }

                SecretKey key;
                try {
                    KeyGenerator gen = KeyGenerator.getInstance("AES");
                    gen.init(128);
                    key = gen.generateKey();
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("Failed to generate shared key.", e);
                }

                SessionService sessionService = session.getFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());
                String serverId = SessionService.getServerId(helloPacket.getServerId(), helloPacket.getPublicKey(), key);

                // TODO: Add generic error, disabled multiplayer and banned from playing online errors
                try {
                    if (helloPacket.isShouldAuthenticate()) {
                        sessionService.joinServer(Objects.requireNonNull(profile, "final shouldAuthenticate changed value?"), accessToken, serverId);
                    }
                } catch (IOException e) {
                    session.disconnect(Component.translatable("disconnect.loginFailedInfo", Component.text(e.getMessage())), e);
                    return;
                }

                session.send(new ServerboundKeyPacket(helloPacket.getPublicKey(), key, helloPacket.getChallenge()),
                    () -> session.setEncryption(protocol.createEncryption(key)));
            } else if (packet instanceof ClientboundLoginFinishedPacket) {
                session.switchInboundState(() -> protocol.setInboundState(ProtocolState.CONFIGURATION));
                session.send(new ServerboundLoginAcknowledgedPacket());
                session.switchOutboundState(() -> protocol.setOutboundState(ProtocolState.CONFIGURATION));
            } else if (packet instanceof ClientboundLoginDisconnectPacket loginDisconnectPacket) {
                session.disconnect(loginDisconnectPacket.getReason());
            } else if (packet instanceof ClientboundLoginCompressionPacket loginCompressionPacket) {
                int threshold = loginCompressionPacket.getThreshold();
                if (threshold >= 0) {
                    session.setCompression(new CompressionConfig(threshold, new ZlibCompression(), false));
                }
            }
        } else if (protocol.getInboundState() == ProtocolState.STATUS) {
            if (packet instanceof ClientboundStatusResponsePacket statusResponsePacket) {
                ServerStatusInfo info = statusResponsePacket.parseInfo();
                ServerInfoHandler handler = session.getFlag(MinecraftConstants.SERVER_INFO_HANDLER_KEY);
                if (handler != null) {
                    handler.handle(session, info);
                }

                session.send(new ServerboundPingRequestPacket(System.currentTimeMillis()));
            } else if (packet instanceof ClientboundPongResponsePacket pongResponsePacket) {
                long time = System.currentTimeMillis() - pongResponsePacket.getPingTime();
                ServerPingTimeHandler handler = session.getFlag(MinecraftConstants.SERVER_PING_TIME_HANDLER_KEY);
                if (handler != null) {
                    handler.handle(session, time);
                }

                session.disconnect(Component.translatable("multiplayer.status.finished"));
            }
        } else if (protocol.getInboundState() == ProtocolState.GAME) {
            if (packet instanceof ClientboundKeepAlivePacket keepAlivePacket && session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
                session.send(new ServerboundKeepAlivePacket(keepAlivePacket.getPingId()));
            } else if (packet instanceof ClientboundDisconnectPacket disconnectPacket) {
                session.disconnect(disconnectPacket.getReason());
            } else if (packet instanceof ClientboundStartConfigurationPacket) {
                session.switchInboundState(() -> protocol.setInboundState(ProtocolState.CONFIGURATION));
                session.send(new ServerboundConfigurationAcknowledgedPacket());
                session.switchOutboundState(() -> protocol.setOutboundState(ProtocolState.CONFIGURATION));
            } else if (packet instanceof ClientboundTransferPacket transferPacket) {
                if (session.getFlag(MinecraftConstants.FOLLOW_TRANSFERS, true)) {
                    ClientNetworkSession newSession = new ClientNetworkSession(
                        InetSocketAddress.createUnresolved(transferPacket.getHost(), transferPacket.getPort()),
                        session.getPacketProtocol(),
                        session.getPacketHandlerExecutor(),
                        session.getLocalAddress(),
                        ((ClientSession) session).getProxy()
                    );
                    newSession.setFlags(session.getFlags());
                    newSession.setFlag(BuiltinFlags.CLIENT_TRANSFERRING, true);
                    session.disconnect(Component.translatable("disconnect.transfer"));
                    newSession.connect(true);
                }
            }
        } else if (protocol.getInboundState() == ProtocolState.CONFIGURATION) {
            if (packet instanceof ClientboundKeepAlivePacket keepAlivePacket && session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
                session.send(new ServerboundKeepAlivePacket(keepAlivePacket.getPingId()));
            } else if (packet instanceof ClientboundFinishConfigurationPacket) {
                session.switchInboundState(() -> protocol.setInboundState(ProtocolState.GAME));
                session.send(new ServerboundFinishConfigurationPacket());
                session.switchOutboundState(() -> protocol.setOutboundState(ProtocolState.GAME));
            } else if (packet instanceof ClientboundSelectKnownPacks) {
                if (session.getFlag(MinecraftConstants.SEND_BLANK_KNOWN_PACKS_RESPONSE, true)) {
                    session.send(new ServerboundSelectKnownPacks(Collections.emptyList()));
                }
            } else if (packet instanceof ClientboundTransferPacket transferPacket) {
                if (session.getFlag(MinecraftConstants.FOLLOW_TRANSFERS, true)) {
                    ClientNetworkSession newSession = new ClientNetworkSession(
                        InetSocketAddress.createUnresolved(transferPacket.getHost(), transferPacket.getPort()),
                        session.getPacketProtocol(),
                        session.getPacketHandlerExecutor(),
                        session.getLocalAddress(),
                        ((ClientSession) session).getProxy()
                    );
                    newSession.setFlags(session.getFlags());
                    newSession.setFlag(BuiltinFlags.CLIENT_TRANSFERRING, true);
                    session.disconnect(Component.translatable("disconnect.transfer"));
                    newSession.connect(true);
                }
            }
        }
    }

    @Override
    public void connected(ConnectedEvent event) {
        Session session = event.getSession();
        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        ClientIntentionPacket intention = new ClientIntentionPacket(
            protocol.getCodec().getProtocolVersion(),
            session.getFlagSupplied(MinecraftConstants.CLIENT_HOST, () -> ((InetSocketAddress) session.getRemoteAddress()).getHostString()),
            session.getFlagSupplied(MinecraftConstants.CLIENT_PORT, () -> ((InetSocketAddress) session.getRemoteAddress()).getPort()),
            handshakeIntent
        );

        var targetState = switch (handshakeIntent) {
            case LOGIN, TRANSFER -> ProtocolState.LOGIN;
            case STATUS -> ProtocolState.STATUS;
        };
        session.switchInboundState(() -> protocol.setInboundState(targetState));
        session.send(intention);
        session.switchOutboundState(() -> protocol.setOutboundState(targetState));
        switch (targetState) {
            case LOGIN -> {
                GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                session.send(new ServerboundHelloPacket(profile.getName(), profile.getId()));
            }
            case STATUS -> session.send(new ServerboundStatusRequestPacket());
            default -> throw new IllegalStateException("Unexpected value: " + targetState);
        }
    }
}
