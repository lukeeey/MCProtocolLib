package org.geysermc.mcprotocollib.network.factory;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.netty.DefaultPacketHandlerExecutor;
import org.geysermc.mcprotocollib.network.session.ClientNetworkSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.Executor;

@Setter
@Accessors(chain = true)
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ClientNetworkSessionFactory {
    private SocketAddress remoteSocketAddress;
    private MinecraftProtocol protocol;
    private Executor packetHandlerExecutor;
    private SocketAddress bindSocketAddress;
    private ProxyInfo proxy;

    public static ClientNetworkSessionFactory factory() {
        return new ClientNetworkSessionFactory();
    }

    public ClientNetworkSessionFactory setAddress(String address) {
        return setAddress(address, 25565);
    }

    public ClientNetworkSessionFactory setAddress(String address, int port) {
        return setRemoteSocketAddress(InetSocketAddress.createUnresolved(address, port));
    }

    public ClientNetworkSessionFactory setBindAddress(String bindAddress) {
        return setBindAddress(bindAddress, 0);
    }

    public ClientNetworkSessionFactory setBindAddress(String bindAddress, int port) {
        return setRemoteSocketAddress(InetSocketAddress.createUnresolved(bindAddress, port));
    }

    public ClientNetworkSession create() {
        return new ClientNetworkSession(
            Objects.requireNonNull(remoteSocketAddress, "socketRemoteAddress"),
            Objects.requireNonNull(protocol, "protocol"),
            Objects.requireNonNullElseGet(packetHandlerExecutor, DefaultPacketHandlerExecutor::createExecutor),
            bindSocketAddress,
            proxy
        );
    }
}
