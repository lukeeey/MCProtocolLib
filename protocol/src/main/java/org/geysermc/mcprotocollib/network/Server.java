package org.geysermc.mcprotocollib.network;

import org.geysermc.mcprotocollib.network.event.server.ServerListener;
import org.geysermc.mcprotocollib.network.packet.PacketProtocol;
import org.geysermc.mcprotocollib.network.server.AbstractServer;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Listens for new sessions to connect.
 */
public interface Server {
    /**
     * Gets the bind address the server is listening on.
     *
     * @return The listening bind address.
     */
    SocketAddress getBindAddress();

    /**
     * Gets the packet protocol of the server.
     *
     * @return The server's packet protocol.
     */
    Supplier<? extends PacketProtocol> getPacketProtocol();

    /**
     * Returns true if the listener is listening.
     *
     * @return True if the listener is listening.
     */
    boolean isListening();

    /**
     * Gets this server's set flags.
     *
     * @return This server's flags.
     */
    Map<String, Object> getGlobalFlags();

    /**
     * Checks whether this server has a flag set.
     *
     * @param flag Flag to check for.
     * @return Whether this server has a flag set.
     */
    boolean hasGlobalFlag(Flag<?> flag);

    /**
     * Gets the value of the given flag as an instance of the given type.
     *
     * @param <T> Type of the flag.
     * @param flag Flag to check for.
     * @return Value of the flag.
     * @throws IllegalStateException If the flag's value isn't of the required type.
     */
    default <T> T getGlobalFlag(Flag<T> flag) {
        return getGlobalFlagSupplied(flag, () -> null);
    }

    /**
     * @see #getGlobalFlagSupplied(Flag, Supplier)
     */
    default <T> T getGlobalFlag(Flag<T> flag, T def) {
        return getGlobalFlagSupplied(flag, () -> def);
    }

    /**
     * Gets the value of the given flag as an instance of the given type.
     * If the flag is not set, the specified default value will be returned.
     *
     * @param <T> Type of the flag.
     * @param flag Flag to check for.
     * @param defSupplier Default value supplier.
     * @return Value of the flag.
     * @throws IllegalStateException If the flag's value isn't of the required type.
     */
    <T> T getGlobalFlagSupplied(Flag<T> flag, Supplier<T> defSupplier);

    /**
     * Sets the value of a flag. The flag will be used in sessions if a session does
     * not contain a value for the flag.
     *
     * @param <T> Type of the flag.
     * @param flag Flag to check for.
     * @param value Value to set the flag to.
     */
    <T> void setGlobalFlag(Flag<T> flag, T value);

    /**
     * Gets the listeners listening on this session.
     *
     * @return This server's listeners.
     */
    List<ServerListener> getListeners();

    /**
     * Adds a listener to this server.
     *
     * @param listener Listener to add.
     */
    void addListener(ServerListener listener);

    /**
     * Removes a listener from this server.
     *
     * @param listener Listener to remove.
     */
    void removeListener(ServerListener listener);

    /**
     * Gets all sessions belonging to this server.
     *
     * @return Sessions belonging to this server.
     */
    List<Session> getSessions();

    /**
     * Binds the listener to its host and port.
     *
     * @return The server.
     */
    AbstractServer bind();

    /**
     * Binds the listener to its host and port.
     *
     * @param wait Whether to wait for the listener to finish binding.
     * @return The server.
     */
    AbstractServer bind(boolean wait);

    /**
     * Binds the listener to its host and port.
     *
     * @param wait Whether to wait for the listener to finish binding.
     * @param callback Callback to call when the listener has finished binding.
     * @return The server.
     */
    AbstractServer bind(boolean wait, Runnable callback);

    /**
     * Closes the listener.
     */
    void close();

    /**
     * Closes the listener.
     *
     * @param wait Whether to wait for the listener to finish closing.
     */
    void close(boolean wait);

    /**
     * Closes the listener.
     *
     * @param wait Whether to wait for the listener to finish closing.
     * @param callback Callback to call when the listener has finished closing.
     */
    void close(boolean wait, Runnable callback);
}
