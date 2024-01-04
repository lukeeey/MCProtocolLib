package com.github.steveice10.mc.protocol.codec;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.codec.PacketCodecHelper;
import com.github.steveice10.packetlib.codec.PacketDefinition;
import com.github.steveice10.packetlib.packet.PacketHeader;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Map;

public class PacketStateCodec extends PacketProtocol {

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getSRVRecordPrefix() {
        return MinecraftConstants.SRV_RECORD_PREFIX;
    }

    @Override
    public PacketHeader getPacketHeader() {
        return MinecraftConstants.PACKET_HEADER;
    }

    @Override
    public PacketCodecHelper createHelper() {
        throw new UnsupportedOperationException("Not supported!");
    }

    @Override
    public void newClientSession(Session session, boolean transferring) {
        throw new UnsupportedOperationException("Not supported!");
    }

    @Override
    public void newServerSession(Server server, Session session) {
        throw new UnsupportedOperationException("Not supported!");
    }

    public static class Builder {
        private final Int2ObjectMap<PacketDefinition<? extends MinecraftPacket, MinecraftCodecHelper>> clientboundPackets = new Int2ObjectOpenHashMap<>();
        private final Int2ObjectMap<PacketDefinition<? extends MinecraftPacket, MinecraftCodecHelper>> serverboundPackets = new Int2ObjectOpenHashMap<>();

        private int nextClientboundId = 0x00;
        private int nextServerboundId = 0x00;

        public <T extends MinecraftPacket> Builder registerClientboundPacket(int id, Class<T> packetClass, PacketFactory<T, MinecraftCodecHelper> factory) {
            this.nextClientboundId = id;
            return registerClientboundPacket(packetClass, factory);
        }

        public <T extends MinecraftPacket> Builder registerClientboundPacket(Class<T> packetClass, PacketFactory<T, MinecraftCodecHelper> factory) {
            this.clientboundPackets.put(nextClientboundId, new PacketDefinition<>(nextClientboundId, packetClass, new MinecraftPacketSerializer<>(factory)));
            this.nextClientboundId++;
            return this;
        }

        public <T extends MinecraftPacket> Builder registerServerboundPacket(int id, Class<T> packetClass, PacketFactory<T, MinecraftCodecHelper> factory) {
            this.nextServerboundId = id;
            return registerServerboundPacket(packetClass, factory);
        }

        public <T extends MinecraftPacket> Builder registerServerboundPacket(Class<T> packetClass, PacketFactory<T, MinecraftCodecHelper> factory) {
            this.serverboundPackets.put(nextServerboundId, new PacketDefinition<>(nextServerboundId, packetClass, new MinecraftPacketSerializer<>(factory)));
            this.nextServerboundId++;
            return this;
        }

        public PacketStateCodec build() {
            PacketStateCodec codec = new PacketStateCodec();
            for (Int2ObjectMap.Entry<PacketDefinition<? extends MinecraftPacket, MinecraftCodecHelper>> entry : this.clientboundPackets.int2ObjectEntrySet()) {
                codec.registerClientbound(entry.getValue());
            }

            for (Int2ObjectMap.Entry<PacketDefinition<? extends MinecraftPacket, MinecraftCodecHelper>> entry : this.serverboundPackets.int2ObjectEntrySet()) {
                codec.registerServerbound(entry.getValue());
            }

            return codec;
        }
    }
}
