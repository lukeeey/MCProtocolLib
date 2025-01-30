package org.geysermc.mcprotocollib.network.example;

import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectingEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSessionListener extends SessionAdapter {
    private static final Logger log = LoggerFactory.getLogger(ClientSessionListener.class);

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof PingPacket pingPacket) {
            String id = pingPacket.getPingId();

            log.info("CLIENT Received: {}", id);

            if (id.equals("hello")) {
                session.send(new PingPacket("exit"));
            } else if (id.equals("exit")) {
                session.disconnect(Component.text("Finished"));
            }
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        if (packet instanceof PingPacket pingPacket) {
            log.info("CLIENT Sent: {}", pingPacket.getPingId());
        }
    }

    @Override
    public void connected(ConnectedEvent event) {
        log.info("CLIENT Connected");

        event.getSession().setEncryption(((TestProtocol) event.getSession().getPacketProtocol()).getEncryption());
        event.getSession().send(new PingPacket("hello"));
    }

    @Override
    public void disconnecting(DisconnectingEvent event) {
        log.info("CLIENT Disconnecting: {}", event.getDetails().reason());
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        log.info("CLIENT Disconnected: {}", event.getDetails().reason());
    }
}
