package org.geysermc.mcprotocollib.network.helper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DefaultDnsRecordDecoder;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.codec.haproxy.HAProxyProxiedProtocol;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.network.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.function.Supplier;

public class NettyHelper {
    private static final Logger log = LoggerFactory.getLogger(NettyHelper.class);
    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";

    public static SocketAddress resolveAddress(Session session, Supplier<EventLoop> eventLoop, SocketAddress address) {
        if (address instanceof InetSocketAddress inetAddress && inetAddress.isUnresolved()) {
            return resolveAddress(session, eventLoop.get(), inetAddress.getHostString(), inetAddress.getPort());
        }

        return address;
    }

    public static SocketAddress resolveAddress(Session session, EventLoop eventLoop, String host, int port) {
        String name = session.getPacketProtocol().getSRVRecordPrefix() + "._tcp." + host;
        log.debug("Attempting SRV lookup for \"{}\".", name);

        if (session.getFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, true) && (!host.matches(IP_REGEX) && !host.equalsIgnoreCase("localhost"))) {
            try (DnsNameResolver resolver = new DnsNameResolverBuilder(eventLoop)
                .channelFactory(TransportHelper.TRANSPORT_TYPE.datagramChannelFactory())
                .build()) {
                AddressedEnvelope<DnsResponse, InetSocketAddress> envelope = resolver.query(new DefaultDnsQuestion(name, DnsRecordType.SRV)).get();
                try {
                    DnsResponse response = envelope.content();
                    if (response.count(DnsSection.ANSWER) > 0) {
                        DefaultDnsRawRecord record = response.recordAt(DnsSection.ANSWER, 0);
                        if (record.type() == DnsRecordType.SRV) {
                            ByteBuf buf = record.content();
                            buf.skipBytes(4); // Skip priority and weight.

                            int tempPort = buf.readUnsignedShort();
                            String tempHost = DefaultDnsRecordDecoder.decodeName(buf);
                            if (tempHost.endsWith(".")) {
                                tempHost = tempHost.substring(0, tempHost.length() - 1);
                            }

                            log.debug("Found SRV record containing \"{}:{}\".", tempHost, tempPort);

                            host = tempHost;
                            port = tempPort;
                        } else {
                            log.debug("Received non-SRV record in response.");
                        }
                    } else {
                        log.debug("No SRV record found.");
                    }
                } finally {
                    envelope.release();
                }
            } catch (Exception e) {
                log.debug("Failed to resolve SRV record.", e);
            }
        } else {
            log.debug("Not resolving SRV record for {}", host);
        }

        // Resolve host here
        try {
            InetAddress resolved = InetAddress.getByName(host);
            log.debug("Resolved {} -> {}", host, resolved.getHostAddress());
            return new InetSocketAddress(resolved, port);
        } catch (UnknownHostException e) {
            log.debug("Failed to resolve host, letting Netty do it instead.", e);
            return InetSocketAddress.createUnresolved(host, port);
        }
    }
    
    public static void initializeHAProxySupport(Session session, Channel channel) {
        InetSocketAddress clientAddress = session.getFlag(BuiltinFlags.CLIENT_PROXIED_ADDRESS);
        if (clientAddress == null) {
            return;
        }

        channel.pipeline().addLast("proxy-protocol-encoder", HAProxyMessageEncoder.INSTANCE);
        channel.pipeline().addLast("proxy-protocol-packet-sender", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                HAProxyProxiedProtocol proxiedProtocol = getProxiedProtocol(clientAddress);

                SocketAddress remoteAddress = ctx.channel().remoteAddress();
                String destinationAddress;
                int destinationPort;
                if (remoteAddress instanceof InetSocketAddress inetRemoteAddress && getProxiedProtocol(inetRemoteAddress) == proxiedProtocol) {
                    destinationAddress = inetRemoteAddress.getAddress().getHostAddress();
                    destinationPort = inetRemoteAddress.getPort();
                } else {
                    // Fill in arbitrary values for the destination address and port if the remote address is not of the same type
                    switch (proxiedProtocol) {
                        case TCP4 -> {
                            destinationAddress = "0.0.0.0";
                            destinationPort = 0;
                        }
                        case TCP6 -> {
                            destinationAddress = "0:0:0:0:0:0:0:0";
                            destinationPort = 0;
                        }
                        default -> throw new UnsupportedOperationException("Unsupported proxied protocol: " + proxiedProtocol);
                    }

                    log.debug("Remote address {} is not of the same type as the client address {} - using arbitrary values for destination address and port", remoteAddress, clientAddress);
                }

                ctx.channel().writeAndFlush(new HAProxyMessage(
                    HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, proxiedProtocol,
                    clientAddress.getAddress().getHostAddress(), destinationAddress,
                    clientAddress.getPort(), destinationPort
                )).addListener(future -> channel.pipeline().remove("proxy-protocol-encoder"));
                ctx.pipeline().remove(this);

                super.channelActive(ctx);
            }
        });
    }

    private static HAProxyProxiedProtocol getProxiedProtocol(InetSocketAddress socketAddress) {
        if (socketAddress.getAddress() instanceof Inet4Address) {
            return HAProxyProxiedProtocol.TCP4;
        } else if (socketAddress.getAddress() instanceof Inet6Address) {
            return HAProxyProxiedProtocol.TCP6;
        } else {
            return HAProxyProxiedProtocol.UNKNOWN;
        }
    }

    public static void addProxy(ProxyInfo proxy, ChannelPipeline pipeline) {
        if (proxy == null) {
            return;
        }

        switch (proxy.type()) {
            case HTTP -> {
                if (proxy.username() != null && proxy.password() != null) {
                    pipeline.addLast("proxy", new HttpProxyHandler(proxy.address(), proxy.username(), proxy.password()));
                } else {
                    pipeline.addLast("proxy", new HttpProxyHandler(proxy.address()));
                }
            }
            case SOCKS4 -> {
                if (proxy.username() != null) {
                    pipeline.addLast("proxy", new Socks4ProxyHandler(proxy.address(), proxy.username()));
                } else {
                    pipeline.addLast("proxy", new Socks4ProxyHandler(proxy.address()));
                }
            }
            case SOCKS5 -> {
                if (proxy.username() != null && proxy.password() != null) {
                    pipeline.addLast("proxy", new Socks5ProxyHandler(proxy.address(), proxy.username(), proxy.password()));
                } else {
                    pipeline.addLast("proxy", new Socks5ProxyHandler(proxy.address()));
                }
            }
            default -> throw new UnsupportedOperationException("Unsupported proxy type: " + proxy.type());
        }
    }
}
