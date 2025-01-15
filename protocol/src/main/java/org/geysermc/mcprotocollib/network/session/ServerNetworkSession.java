package org.geysermc.mcprotocollib.network.session;

import io.netty.channel.ChannelHandlerContext;
import org.geysermc.mcprotocollib.network.Flag;
import org.geysermc.mcprotocollib.network.ServerSession;
import org.geysermc.mcprotocollib.network.codec.PacketCodecHelper;
import org.geysermc.mcprotocollib.network.server.NetworkServer;
import org.geysermc.mcprotocollib.network.packet.PacketProtocol;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class ServerNetworkSession extends NetworkSession implements ServerSession {
    private final NetworkServer server;
    private final PacketCodecHelper codecHelper;

    public ServerNetworkSession(SocketAddress remoteAddress, PacketProtocol protocol, NetworkServer server, Executor packetHandlerExecutor) {
        super(remoteAddress, protocol, packetHandlerExecutor);
        this.server = server;
        this.codecHelper = protocol.createHelper();
    }

    @Override
    public PacketCodecHelper getCodecHelper() {
        return this.codecHelper;
    }

    @Override
    public Map<String, Object> getFlags() {
        Map<String, Object> ret = new HashMap<>();
        ret.putAll(this.server.getGlobalFlags());
        ret.putAll(super.getFlags());
        return ret;
    }

    @Override
    public boolean hasFlag(Flag<?> flag) {
        if (super.hasFlag(flag)) {
            return true;
        }

        return this.server.hasGlobalFlag(flag);
    }

    @Override
    public <T> T getFlagSupplied(Flag<T> flag, Supplier<T> defSupplier) {
        T ret = super.getFlagSupplied(flag, () -> null);
        if (ret != null) {
            return ret;
        }

        return this.server.getGlobalFlagSupplied(flag, defSupplier);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.server.addSession(this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        this.server.removeSession(this);
    }
}
