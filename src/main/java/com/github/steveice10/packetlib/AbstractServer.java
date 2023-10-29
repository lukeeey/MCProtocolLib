package com.github.steveice10.packetlib;

import com.github.steveice10.packetlib.event.server.*;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Supplier;

@RequiredArgsConstructor
public abstract class AbstractServer implements Server {
    @Getter
    private final String host;
    @Getter
    private final int port;
    @Getter
    private final Supplier<? extends PacketProtocol> protocolSupplier;

    private final List<Session> sessions = new ArrayList<>();

    private final Map<Flag<?>, Object> flags = new HashMap<>();
    private final List<ServerListener> listeners = new ArrayList<>();

    protected PacketProtocol createPacketProtocol() {
        return this.protocolSupplier.get();
    }

    @Override
    public Map<Flag<?>, ?> getGlobalFlags() {
        return Collections.unmodifiableMap(this.flags);
    }

    @Override
    public <T> boolean hasGlobalFlag(Flag<T> key) {
        return this.flags.containsKey(key);
    }

    @Override
    public <T> T getGlobalFlag(Flag<T> key) {
        return this.getGlobalFlag(key, null);
    }

    @Override
    public <T> T getGlobalFlag(Flag<T> key, T def) {
        Object value = this.flags.get(key);
        if (value == null) {
            return def;
        }

        return key.cast(value);
    }

    @Override
    public <T> void setGlobalFlag(Flag<T> key, T value) {
        this.flags.put(key, value);
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
            session.disconnect("Connection closed.");
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
                session.disconnect("Server closed.");
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
