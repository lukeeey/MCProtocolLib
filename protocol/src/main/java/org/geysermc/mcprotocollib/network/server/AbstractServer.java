package org.geysermc.mcprotocollib.network.server;

import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.Flag;
import org.geysermc.mcprotocollib.network.Server;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.server.ServerBoundEvent;
import org.geysermc.mcprotocollib.network.event.server.ServerClosedEvent;
import org.geysermc.mcprotocollib.network.event.server.ServerClosingEvent;
import org.geysermc.mcprotocollib.network.event.server.ServerEvent;
import org.geysermc.mcprotocollib.network.event.server.ServerListener;
import org.geysermc.mcprotocollib.network.event.server.SessionAddedEvent;
import org.geysermc.mcprotocollib.network.event.server.SessionRemovedEvent;
import org.geysermc.mcprotocollib.network.packet.PacketProtocol;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractServer implements Server {
    private final SocketAddress bindAddress;
    private final Supplier<? extends MinecraftProtocol> protocolSupplier;

    private final List<Session> sessions = new ArrayList<>();

    private final Map<String, Object> flags = new HashMap<>();
    private final List<ServerListener> listeners = new ArrayList<>();

    public AbstractServer(SocketAddress bindAddress, Supplier<? extends MinecraftProtocol> protocolSupplier) {
        this.bindAddress = bindAddress;
        this.protocolSupplier = protocolSupplier;
    }

    @Override
    public SocketAddress getBindAddress() {
        return this.bindAddress;
    }

    @Override
    public Supplier<? extends PacketProtocol> getPacketProtocol() {
        return this.protocolSupplier;
    }

    protected MinecraftProtocol createPacketProtocol() {
        return this.protocolSupplier.get();
    }

    @Override
    public Map<String, Object> getGlobalFlags() {
        return Collections.unmodifiableMap(this.flags);
    }

    @Override
    public boolean hasGlobalFlag(Flag<?> flag) {
        return this.flags.containsKey(flag.key());
    }

    @Override
    public <T> T getGlobalFlagSupplied(Flag<T> flag, Supplier<T> defSupplier) {
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
    public <T> void setGlobalFlag(Flag<T> flag, T value) {
        this.flags.put(flag.key(), value);
    }

    @Override
    public List<ServerListener> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }

    @Override
    public void addListener(ServerListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(ServerListener listener) {
        this.listeners.remove(listener);
    }

    protected void callEvent(ServerEvent event) {
        for (ServerListener listener : this.listeners) {
            event.call(listener);
        }
    }

    @Override
    public List<Session> getSessions() {
        return new ArrayList<>(this.sessions);
    }

    public void addSession(Session session) {
        this.sessions.add(session);
        this.callEvent(new SessionAddedEvent(this, session));
    }

    public void removeSession(Session session) {
        this.sessions.remove(session);
        if (session.isConnected()) {
            session.disconnect(Component.translatable("disconnect.endOfStream"));
        }

        this.callEvent(new SessionRemovedEvent(this, session));
    }

    @Override
    public AbstractServer bind() {
        return this.bind(true);
    }

    @Override
    public AbstractServer bind(boolean wait) {
        return this.bind(wait, null);
    }

    @Override
    public AbstractServer bind(boolean wait, Runnable callback) {
        this.bindImpl(wait, () -> {
            callEvent(new ServerBoundEvent(AbstractServer.this));
            if (callback != null) {
                callback.run();
            }
        });

        return this;
    }

    protected abstract void bindImpl(boolean wait, Runnable callback);

    @Override
    public void close() {
        this.close(true);
    }

    @Override
    public void close(boolean wait) {
        this.close(wait, null);
    }

    @Override
    public void close(boolean wait, Runnable callback) {
        this.callEvent(new ServerClosingEvent(this));
        for (Session session : this.getSessions()) {
            if (session.isConnected()) {
                session.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
            }
        }

        this.closeImpl(wait, () -> {
            callEvent(new ServerClosedEvent(AbstractServer.this));
            if (callback != null) {
                callback.run();
            }
        });
    }

    protected abstract void closeImpl(boolean wait, Runnable callback);
}
