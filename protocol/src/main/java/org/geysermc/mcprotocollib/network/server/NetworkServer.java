package org.geysermc.mcprotocollib.network.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.helper.TransportHelper;
import org.geysermc.mcprotocollib.network.netty.DefaultPacketHandlerExecutor;
import org.geysermc.mcprotocollib.network.netty.MinecraftChannelInitializer;
import org.geysermc.mcprotocollib.network.session.NetworkSession;
import org.geysermc.mcprotocollib.network.session.ServerNetworkSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class NetworkServer extends AbstractServer {
    private static final Logger log = LoggerFactory.getLogger(NetworkServer.class);

    private final Supplier<Executor> packetHandlerExecutorFactory;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public NetworkServer(SocketAddress bindAddress, Supplier<? extends MinecraftProtocol> protocol) {
        this(bindAddress, protocol, DefaultPacketHandlerExecutor::createExecutor);
    }

    public NetworkServer(SocketAddress bindAddress, Supplier<? extends MinecraftProtocol> protocol, Supplier<Executor> packetHandlerExecutorFactory) {
        super(bindAddress, protocol);
        this.packetHandlerExecutorFactory = packetHandlerExecutorFactory;
    }

    @Override
    public boolean isListening() {
        return this.channel != null && this.channel.isOpen();
    }

    @Override
    public void bindImpl(boolean wait, final Runnable callback) {
        if (this.bossGroup != null || this.workerGroup != null || this.channel != null) {
            return;
        }

        this.bossGroup = createBossEventLoopGroup();
        this.workerGroup = createWorkerEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .channelFactory(getChannelFactory())
                .group(this.bossGroup, this.workerGroup)
                .localAddress(this.getBindAddress())
                .childHandler(getChannelHandler());

        setOptions(bootstrap);

        CompletableFuture<Void> handleFuture = new CompletableFuture<>();
        bootstrap.bind().addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                this.channel = future.channel();
                if (callback != null) {
                    callback.run();
                }
            } else {
                log.error("Failed to bind connection listener.", future.cause());
            }

            handleFuture.complete(null);
        });

        if (wait) {
            handleFuture.join();
        }
    }

    protected ChannelFactory<? extends ServerChannel> getChannelFactory() {
        return TransportHelper.TRANSPORT_TYPE.serverSocketChannelFactory();
    }

    protected EventLoopGroup createBossEventLoopGroup() {
        return TransportHelper.TRANSPORT_TYPE.eventLoopGroupFactory().apply(null);
    }

    protected EventLoopGroup createWorkerEventLoopGroup() {
        return TransportHelper.TRANSPORT_TYPE.eventLoopGroupFactory().apply(null);
    }

    protected void setOptions(ServerBootstrap bootstrap) {
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.IP_TOS, 0x18);

        if (getGlobalFlag(BuiltinFlags.TCP_FAST_OPEN, false) && TransportHelper.TRANSPORT_TYPE.supportsTcpFastOpenServer()) {
            bootstrap.option(ChannelOption.TCP_FASTOPEN, 3);
        }
    }

    protected ChannelHandler getChannelHandler() {
        return new MinecraftChannelInitializer<>(channel -> {
            MinecraftProtocol protocol = createPacketProtocol();

            NetworkSession session = new ServerNetworkSession(channel.remoteAddress(), protocol, NetworkServer.this, packetHandlerExecutorFactory.get());
            session.getPacketProtocol().newServerSession(NetworkServer.this, session);

            return session;
        }, false);
    }

    @Override
    public void closeImpl(boolean wait, final Runnable callback) {
        closeChannel(callback, wait);
        closeWorkerGroup(wait);
        closeBossGroup(wait);
    }

    private void closeChannel(Runnable callback, boolean wait) {
        if (this.channel == null) {
            return;
        }

        if (this.channel.isOpen()) {
            CompletableFuture<Void> handleFuture = new CompletableFuture<>();
            this.channel.close().addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    if (callback != null) {
                        callback.run();
                    }
                } else {
                    log.error("Failed to close connection listener.", future.cause());
                }

                handleFuture.complete(null);
            });

            if (wait) {
                handleFuture.join();
            }
        }

        this.channel = null;
    }

    private void closeWorkerGroup(boolean wait) {
        if (this.workerGroup == null) {
            return;
        }

        CompletableFuture<Void> handleFuture = new CompletableFuture<>();
        this.workerGroup.shutdownGracefully().addListener(future -> {
            if (!future.isSuccess()) {
                log.debug("Failed to close connection listener.", future.cause());
            }

            handleFuture.complete(null);
        });

        if (wait) {
            handleFuture.join();
        }

        this.workerGroup = null;
    }

    private void closeBossGroup(boolean wait) {
        if (this.bossGroup == null) {
            return;
        }

        CompletableFuture<Void> handleFuture = new CompletableFuture<>();
        this.bossGroup.shutdownGracefully().addListener(future -> {
            if (!future.isSuccess()) {
                log.debug("Failed to close connection listener.", future.cause());
            }

            handleFuture.complete(null);
        });

        if (wait) {
            handleFuture.join();
        }

        this.bossGroup = null;
    }
}
