package org.geysermc.mcprotocollib.network.example;

import io.netty.buffer.ByteBuf;
import org.geysermc.mcprotocollib.network.Server;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.codec.BasePacketCodecHelper;
import org.geysermc.mcprotocollib.network.codec.PacketCodecHelper;
import org.geysermc.mcprotocollib.network.codec.PacketDefinition;
import org.geysermc.mcprotocollib.network.codec.PacketSerializer;
import org.geysermc.mcprotocollib.network.crypt.AESEncryption;
import org.geysermc.mcprotocollib.network.crypt.EncryptionConfig;
import org.geysermc.mcprotocollib.network.packet.DefaultPacketHeader;
import org.geysermc.mcprotocollib.network.packet.PacketHeader;
import org.geysermc.mcprotocollib.network.packet.PacketProtocol;
import org.geysermc.mcprotocollib.network.packet.PacketRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

public class TestProtocol extends PacketProtocol {
    private static final Logger log = LoggerFactory.getLogger(TestProtocol.class);
    private final PacketHeader header = new DefaultPacketHeader();
    private final PacketRegistry registry = new PacketRegistry();
    private EncryptionConfig encrypt;

    @SuppressWarnings("unused")
    public TestProtocol() {
    }

    public TestProtocol(SecretKey key) {
        this.setSecretKey(key);
    }

    public PacketCodecHelper createHelper() {
        return new BasePacketCodecHelper();
    }

    public void setSecretKey(SecretKey key) {
        registry.register(0, PingPacket.class, new PacketSerializer<>() {
            @Override
            public void serialize(ByteBuf buf, PacketCodecHelper helper, PingPacket packet) {
                helper.writeString(buf, packet.getPingId());
            }

            @Override
            public PingPacket deserialize(ByteBuf buf, PacketCodecHelper helper, PacketDefinition<PingPacket, PacketCodecHelper> definition) {
                return new PingPacket(buf, helper);
            }
        });

        try {
            this.encrypt = new EncryptionConfig(new AESEncryption(key));
        } catch (GeneralSecurityException e) {
            log.error("Failed to create encryption", e);
        }
    }

    @Override
    public String getSRVRecordPrefix() {
        return "_test";
    }

    @Override
    public PacketHeader getPacketHeader() {
        return this.header;
    }

    public EncryptionConfig getEncryption() {
        return this.encrypt;
    }

    @Override
    public void newClientSession(Session session) {
        session.addListener(new ClientSessionListener());
    }

    @Override
    public void newServerSession(Server server, Session session) {
        session.addListener(new ServerSessionListener());
    }

    @Override
    public PacketRegistry getInboundPacketRegistry() {
        return registry;
    }

    @Override
    public PacketRegistry getOutboundPacketRegistry() {
        return registry;
    }
}
