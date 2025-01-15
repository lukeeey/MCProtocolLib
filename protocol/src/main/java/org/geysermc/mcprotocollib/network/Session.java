package org.geysermc.mcprotocollib.network;

import io.netty.channel.Channel;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.mcprotocollib.network.codec.PacketCodecHelper;
import org.geysermc.mcprotocollib.network.compression.CompressionConfig;
import org.geysermc.mcprotocollib.network.crypt.EncryptionConfig;
import org.geysermc.mcprotocollib.network.event.session.SessionEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.geysermc.mcprotocollib.network.netty.FlushHandler;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.packet.PacketProtocol;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * A network session.
 */
public interface Session {

    /**
     * Gets the local address of the session.
     *
     * @return The local address, or null if the session is not connected.
     */
    SocketAddress getLocalAddress();

    /**
     * Gets the remote address of the session.
     *
     * @return The remote address, or null if the session is not connected.
     */
    SocketAddress getRemoteAddress();

    /**
     * Gets the packet protocol of the session.
     *
     * @return The session's packet protocol.
     */
    PacketProtocol getPacketProtocol();

    /**
     * Gets the session's {@link PacketCodecHelper}.
     *
     * @return The session's packet codec helper.
     */
    PacketCodecHelper getCodecHelper();

    /**
     * Gets this session's set flags. If this session belongs to a server, the server's
     * flags will be included in the results.
     *
     * @return This session's flags.
     */
    Map<String, Object> getFlags();

    /**
     * Checks whether this session has a flag set. If this session belongs to a server,
     * the server's flags will also be checked.
     *
     * @param flag Flag to check for.
     * @return Whether this session has a flag set.
     */
    boolean hasFlag(Flag<?> flag);

    /**
     * Gets the value of the given flag as an instance of the given type. If this
     * session belongs to a server, the server's flags will be checked for the flag
     * as well.
     *
     * @param <T> Type of the flag.
     * @param flag Flag to check for.
     * @return Value of the flag.
     * @throws IllegalStateException If the flag's value isn't of the required type.
     */
    default <T> T getFlag(Flag<T> flag) {
        return this.getFlagSupplied(flag, () -> null);
    }

    /**
     * @see #getFlagSupplied(Flag, Supplier)
     */
    default <T> T getFlag(Flag<T> flag, T def) {
        return this.getFlagSupplied(flag, () -> def);
    }

    /**
     * Gets the value of the given flag as an instance of the given type. If this
     * session belongs to a server, the server's flags will be checked for the flag
     * as well. If the flag is not set, the specified default value will be returned.
     *
     * @param <T> Type of the flag.
     * @param flag Flag to check for.
     * @param defSupplier Default value supplier.
     * @return Value of the flag.
     * @throws IllegalStateException If the flag's value isn't of the required type.
     */
    <T> T getFlagSupplied(Flag<T> flag, Supplier<T> defSupplier);

    /**
     * Sets the value of a flag. This does not change a server's flags if this session
     * belongs to a server.
     *
     * @param <T> Type of the flag.
     * @param flag Flag to check for.
     * @param value Value to set the flag to.
     */
    <T> void setFlag(Flag<T> flag, T value);

    /**
     * Sets the values for a collection of flags.
     *
     * @param flags Collection of flags
     */
    void setFlags(Map<String, Object> flags);

    /**
     * Gets the listeners listening on this session.
     *
     * @return This session's listeners.
     */
    List<SessionListener> getListeners();

    /**
     * Adds a listener to this session.
     *
     * @param listener Listener to add.
     */
    void addListener(SessionListener listener);

    /**
     * Removes a listener from this session.
     *
     * @param listener Listener to remove.
     */
    void removeListener(SessionListener listener);

    /**
     * Calls an event on the listeners of this session.
     *
     * @param event Event to call.
     */
    void callEvent(SessionEvent event);

    /**
     * Notifies all listeners that a packet was just received.
     *
     * @param packet Packet to notify.
     */
    void callPacketReceived(Packet packet);

    /**
     * Notifies all listeners that a packet was just sent.
     *
     * @param packet Packet to notify.
     */
    void callPacketSent(Packet packet);

    /**
     * Sets the compression config for this session.
     *
     * @param compressionConfig the compression to compress with,
     *                          or null to disable compression
     */
    void setCompression(@Nullable CompressionConfig compressionConfig);

    /**
     * Sets encryption for this session.
     *
     * @param encryptionConfig the encryption to encrypt with,
     *                         or null to disable encryption
     *
     */
    void setEncryption(@Nullable EncryptionConfig encryptionConfig);

    /**
     * Returns true if the session is connected.
     *
     * @return True if the session is connected.
     */
    boolean isConnected();

    /**
     * Sends a packet.
     *
     * @param packet Packet to send.
     */
    default void send(@NonNull Packet packet) {
        this.send(packet, null);
    }

    /**
     * Sends a packet and runs the specified callback when the packet has been sent.
     *
     * @param packet Packet to send.
     * @param onSent Callback to run when the packet has been sent.
     */
    void send(@NonNull Packet packet, @Nullable Runnable onSent);

    /**
     * Disconnects the session.
     * This method just wraps the reason into a {@link Component}.
     * It is recommended to use Components instead as they provide more flexibility.
     *
     * @param reason Reason for disconnecting.
     * @see #disconnect(String, Throwable)
     */
    default void disconnect(@NonNull String reason) {
        this.disconnect(reason, null);
    }

    /**
     * Disconnects the session.
     * This method just wraps the reason into a {@link Component}.
     * It is recommended to use Components instead as they provide more flexibility.
     *
     * @param reason Reason for disconnecting.
     * @param cause Throwable responsible for disconnecting.
     * @see #disconnect(Component, Throwable)
     */
    default void disconnect(@NonNull String reason, @Nullable Throwable cause) {
        this.disconnect(Component.text(reason), cause);
    }

    /**
     * Disconnects the session.
     *
     * @param reason Reason for disconnecting.
     */
    default void disconnect(@NonNull Component reason) {
        this.disconnect(reason, null);
    }

    /**
     * Disconnects the session.
     *
     * @param reason Reason for disconnecting.
     * @param cause Throwable responsible for disconnecting.
     */
    void disconnect(@NonNull Component reason, @Nullable Throwable cause);

    /**
     * Auto read in netty means that the server is automatically reading from the channel.
     * Turning it off means that we won't get more packets being decoded until we turn it back on.
     * We use this to hold off on reading packets until we are ready to process them.
     * For example this is used for switching inbound states with {@link #switchInboundState(Runnable)}.
     *
     * @param autoRead Whether to enable auto read.
     *                 Default is true.
     */
    void setAutoRead(boolean autoRead);

    /**
     * Returns the underlying netty channel of this session.
     *
     * @return The netty channel
     */
    Channel getChannel();

    /**
     * Returns the executor that handles packet handling.
     *
     * @return The packet handler executor
     */
    Executor getPacketHandlerExecutor();

    /**
     * Changes the inbound state of the session and then re-enables auto read.
     * This is used after a terminal packet was handled and the session is ready to receive more packets in the new state.
     *
     * @param switcher The runnable that switches the inbound state.
     */
    default void switchInboundState(Runnable switcher) {
        switcher.run();

        // We switched to the new inbound state
        // we can start reading again
        setAutoRead(true);
    }

    /**
     * Flushes all packets that are due to be sent and changes the outbound state of the session.
     * This makes sure no other threads have scheduled packets to be sent.
     *
     * @param switcher The runnable that switches the outbound state.
     */
    default void switchOutboundState(Runnable switcher) {
        getChannel().writeAndFlush(FlushHandler.FLUSH_PACKET).syncUninterruptibly();

        switcher.run();
    }
}
