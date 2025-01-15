package org.geysermc.mcprotocollib.network.example;

import org.geysermc.mcprotocollib.network.event.server.ServerAdapter;
import org.geysermc.mcprotocollib.network.event.server.ServerBoundEvent;
import org.geysermc.mcprotocollib.network.event.server.ServerClosedEvent;
import org.geysermc.mcprotocollib.network.event.server.ServerClosingEvent;
import org.geysermc.mcprotocollib.network.event.server.SessionAddedEvent;
import org.geysermc.mcprotocollib.network.event.server.SessionRemovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;

public class ServerListener extends ServerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ServerListener.class);
    private final SecretKey key;

    public ServerListener(SecretKey key) {
        this.key = key;
    }

    @Override
    public void serverBound(ServerBoundEvent event) {
        log.info("SERVER Bound: {}", event.getServer().getBindAddress());
    }

    @Override
    public void serverClosing(ServerClosingEvent event) {
        log.info("CLOSING SERVER...");
    }

    @Override
    public void serverClosed(ServerClosedEvent event) {
        log.info("SERVER CLOSED");
    }

    @Override
    public void sessionAdded(SessionAddedEvent event) {
        log.info("SERVER Session Added: {}", event.getSession().getRemoteAddress());
        ((TestProtocol) event.getSession().getPacketProtocol()).setSecretKey(this.key);
        event.getSession().setEncryption(((TestProtocol) event.getSession().getPacketProtocol()).getEncryption());
    }

    @Override
    public void sessionRemoved(SessionRemovedEvent event) {
        log.info("SERVER Session Removed: {}", event.getSession().getRemoteAddress());
        event.getServer().close(false);
    }
}
