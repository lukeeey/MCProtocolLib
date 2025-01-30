package org.geysermc.mcprotocollib.network.helper;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record DisconnectionDetails(@NonNull Component reason, @Nullable Throwable cause) {
    public DisconnectionDetails(@NonNull Component reason) {
        this(reason, null);
    }
}
