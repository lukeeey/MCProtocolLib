package org.geysermc.mcprotocollib.network;

public enum PacketFlow {
    SERVERBOUND,
    CLIENTBOUND;

    public PacketFlow opposite() {
        return this == CLIENTBOUND ? SERVERBOUND : CLIENTBOUND;
    }
}
