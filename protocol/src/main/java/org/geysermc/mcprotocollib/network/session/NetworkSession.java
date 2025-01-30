package org.geysermc.mcprotocollib.network.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.mcprotocollib.network.Flag;
import org.geysermc.mcprotocollib.network.NetworkConstants;
import org.geysermc.mcprotocollib.network.PacketFlow;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.compression.CompressionConfig;
import org.geysermc.mcprotocollib.network.crypt.EncryptionConfig;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectingEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.helper.DisconnectionDetails;
import org.geysermc.mcprotocollib.network.netty.FlushHandler;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public abstract class NetworkSession extends SimpleChannelInboundHandler<Packet> implements Session {
    private static final Logger log = LoggerFactory.getLogger(NetworkSession.class);

    private volatile boolean sendLoginDisconnect = true;
    protected final PacketFlow receiving;
    protected final SocketAddress remoteAddress;
    protected final MinecraftProtocol protocol;
    protected final Executor packetHandlerExecutor;

    private final Map<String, Object> flags = new HashMap<>();
    private final List<SessionListener> listeners = new CopyOnWriteArrayList<>();

    private Channel channel;
    @Nullable
    @Getter
    private DisconnectionDetails disconnectionDetails;
    @Nullable
    @Getter
    private volatile DisconnectionDetails delayedDisconnect;
    @Getter
    protected boolean disconnectionHandled;
    private boolean handlingFault;

    public NetworkSession(SocketAddress remoteAddress, MinecraftProtocol protocol, Executor packetHandlerExecutor, PacketFlow receiving) {
        this.remoteAddress = remoteAddress;
        this.protocol = protocol;
        this.packetHandlerExecutor = packetHandlerExecutor;
        this.receiving = receiving;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return this.channel != null ? this.channel.localAddress() : null;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public MinecraftProtocol getPacketProtocol() {
        return this.protocol;
    }

    @Override
    public Map<String, Object> getFlags() {
        return Collections.unmodifiableMap(this.flags);
    }

    @Override
    public boolean hasFlag(Flag<?> flag) {
        return this.flags.containsKey(flag.key());
    }

    @Override
    public <T> T getFlagSupplied(Flag<T> flag, Supplier<T> defSupplier) {
        Object value = this.flags.get(flag.key());
        if (value == null) {
            return defSupplier.get();
        }

        try {
            return flag.cast(value);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Tried to get flag \"" + flag.key() + "\" as the wrong type. Actual type: " + value.getClass().getName());
        }
    }

    @Override
    public <T> void setFlag(Flag<T> flag, T value) {
        this.flags.put(flag.key(), value);
    }

    @Override
    public void setFlags(Map<String, Object> flags) {
        this.flags.putAll(flags);
    }

    @Override
    public List<SessionListener> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }

    @Override
    public void addListener(SessionListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(SessionListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void callEvent(SessionEvent event) {
        try {
            for (SessionListener listener : this.listeners) {
                event.call(listener);
            }
        } catch (Throwable t) {
            exceptionCaught(null, t);
        }
    }

    @Override
    public void callPacketReceived(Packet packet) {
        try {
            for (SessionListener listener : this.listeners) {
                listener.packetReceived(this, packet);
            }
        } catch (Throwable t) {
            exceptionCaught(null, t);
        }
    }

    @Override
    public void callPacketSent(Packet packet) {
        try {
            for (SessionListener listener : this.listeners) {
                listener.packetSent(this, packet);
            }
        } catch (Throwable t) {
            exceptionCaught(null, t);
        }
    }

    @Override
    public void setCompression(@Nullable CompressionConfig compressionConfig) {
        if (this.channel == null) {
            throw new IllegalStateException("You need to connect to set the compression!");
        }

        log.debug("Setting compression for session {}", this);
        channel.attr(NetworkConstants.COMPRESSION_ATTRIBUTE_KEY).set(compressionConfig);
    }

    @Override
    public void setEncryption(@Nullable EncryptionConfig encryptionConfig) {
        if (channel == null) {
            throw new IllegalStateException("You need to connect to set the encryption!");
        }

        log.debug("Setting encryption for session {}", this);
        channel.attr(NetworkConstants.ENCRYPTION_ATTRIBUTE_KEY).set(encryptionConfig);
    }

    @Override
    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen();
    }

    @Override
    public void send(@NonNull Packet packet, @Nullable Runnable onSent) {
        if (this.channel == null) {
            return;
        }

        // Same behaviour as vanilla, always offload packet sending to the event loop
        if (!this.channel.eventLoop().inEventLoop()) {
            this.channel.eventLoop().execute(() -> this.send(packet, onSent));
            return;
        }

        PacketSendingEvent sendingEvent = new PacketSendingEvent(this, packet);
        this.callEvent(sendingEvent);

        if (!sendingEvent.isCancelled()) {
            final Packet toSend = sendingEvent.getPacket();
            this.channel.writeAndFlush(toSend).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    if (onSent != null) {
                        onSent.run();
                    }

                    callPacketSent(toSend);
                }
            }).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    @Override
    public void disconnect(@NonNull DisconnectionDetails disconnectionDetails) {
        if (this.channel == null) {
            this.delayedDisconnect = disconnectionDetails;
        }

        boolean wasDisconnectionHandled = this.disconnectionHandled;
        if (!wasDisconnectionHandled) {
            this.disconnectionHandled = true;
        }

        if (!wasDisconnectionHandled) {
            this.callEvent(new DisconnectingEvent(this, disconnectionDetails));
        }

        if (this.isConnected()) {
            this.channel.close().awaitUninterruptibly();
            this.disconnectionDetails = disconnectionDetails;
        }

        if (!wasDisconnectionHandled) {
            this.callEvent(new DisconnectedEvent(this, disconnectionDetails));
        }
    }

    @Override
    public void setAutoRead(boolean autoRead) {
        if (this.channel != null) {
            this.channel.config().setAutoRead(autoRead);
        }
    }

    @Override
    public Channel getChannel() {
        return this.channel;
    }

    @Override
    public Executor getPacketHandlerExecutor() {
        return this.packetHandlerExecutor;
    }

    @Override
    public void switchInboundState(ProtocolState state) {
        protocol.setInboundState(state);

        // We switched to the new inbound state
        // we can start reading again
        setAutoRead(true);
    }

    @Override
    public void switchOutboundState(ProtocolState state) {
        getChannel().writeAndFlush(FlushHandler.FLUSH_PACKET).syncUninterruptibly();

        protocol.setOutboundState(state);
        sendLoginDisconnect = state == ProtocolState.LOGIN;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
        DisconnectionDetails delayedDisconnect = this.delayedDisconnect;
        if (delayedDisconnect != null) {
            this.disconnect(delayedDisconnect);
        } else {
            this.callEvent(new ConnectedEvent(this));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.disconnect(Component.translatable("disconnect.endOfStream"));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        boolean wasNotHandlingFault = !this.handlingFault;
        this.handlingFault = true;
        if (this.channel.isOpen()) {
            if (cause instanceof TimeoutException) {
                this.disconnect(Component.translatable("disconnect.timeout"));
            } else {
                Component message = Component.translatable("disconnect.genericReason", Component.text("Internal Exception: " + cause));
                DisconnectionDetails disconnectionDetails = new DisconnectionDetails(message, cause);
                if (wasNotHandlingFault) {
                    if (this.getSending() == PacketFlow.CLIENTBOUND) {
                        Packet packet = this.sendLoginDisconnect ? new ClientboundLoginDisconnectPacket(message) : new ClientboundDisconnectPacket(message);
                        this.send(packet, () -> this.disconnect(disconnectionDetails));
                    } else {
                        this.disconnect(disconnectionDetails);
                    }

                    this.setAutoRead(false);
                } else {
                    this.disconnect(disconnectionDetails);
                }
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        if (this.channel.isOpen()) {
            if (packet.shouldRunOnGameThread()) {
                packetHandlerExecutor.execute(() -> this.callPacketReceived(packet));
            } else {
                this.callPacketReceived(packet);
            }
        }
    }

    public PacketFlow getReceiving() {
        return this.receiving;
    }

    public PacketFlow getSending() {
        return this.receiving.opposite();
    }
}
