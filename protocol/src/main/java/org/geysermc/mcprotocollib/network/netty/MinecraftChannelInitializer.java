package org.geysermc.mcprotocollib.network.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.PacketProtocol;

import java.util.function.Function;

@RequiredArgsConstructor
public class MinecraftChannelInitializer<S extends Session & ChannelHandler> extends ChannelInitializer<Channel> {
    private final Function<Channel, S> sessionFactory;
    private final boolean client;

    @Override
    protected void initChannel(Channel ch) throws Exception {
        S session = createSession(ch);

        addHandlers(session, ch);
    }

    protected S createSession(Channel ch) {
        return sessionFactory.apply(ch);
    }

    protected void addHandlers(S session, Channel ch) {
        PacketProtocol protocol = session.getPacketProtocol();
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("read-timeout", new ReadTimeoutHandler(session.getFlag(BuiltinFlags.READ_TIMEOUT, 30)));
        pipeline.addLast("write-timeout", new WriteTimeoutHandler(session.getFlag(BuiltinFlags.WRITE_TIMEOUT, 0)));

        pipeline.addLast("encryption", new PacketEncryptorCodec());
        pipeline.addLast("sizer", new PacketSizerCodec(protocol.getPacketHeader(), session.getCodecHelper()));
        pipeline.addLast("compression", new PacketCompressionCodec(session.getCodecHelper()));

        pipeline.addLast("flow-control", new AutoReadFlowControlHandler());
        pipeline.addLast("codec", new PacketCodec(session, client));
        pipeline.addLast("flush-handler", new FlushHandler());
        pipeline.addLast("manager", session);
    }
}
