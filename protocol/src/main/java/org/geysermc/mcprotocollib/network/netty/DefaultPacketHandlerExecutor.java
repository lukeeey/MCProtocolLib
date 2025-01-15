package org.geysermc.mcprotocollib.network.netty;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class DefaultPacketHandlerExecutor {
    /**
     * Controls whether non-priority packets are handled in a separate event loop
     */
    public static boolean USE_EVENT_LOOP_FOR_PACKETS = true;
    private static EventLoopGroup PACKET_EVENT_LOOP;
    private static final int SHUTDOWN_QUIET_PERIOD_MS = 100;
    private static final int SHUTDOWN_TIMEOUT_MS = 500;

    public static Executor createExecutor() {
        if (!USE_EVENT_LOOP_FOR_PACKETS) {
            return Runnable::run;
        }

        if (PACKET_EVENT_LOOP == null) {
            // See TcpClientSession.newThreadFactory() for details on
            // daemon threads and their interaction with the runtime.
            PACKET_EVENT_LOOP = new DefaultEventLoopGroup(new DefaultThreadFactory(DefaultPacketHandlerExecutor.class, true));
            Runtime.getRuntime().addShutdownHook(new Thread(
                () -> PACKET_EVENT_LOOP.shutdownGracefully(SHUTDOWN_QUIET_PERIOD_MS, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)));
        }

        return PACKET_EVENT_LOOP.next();
    }
}
