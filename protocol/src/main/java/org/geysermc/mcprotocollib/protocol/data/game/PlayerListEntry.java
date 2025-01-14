package org.geysermc.mcprotocollib.protocol.data.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

import java.security.PublicKey;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PlayerListEntry {
    private final @NonNull UUID profileId;
    private @Nullable GameProfile profile;
    private boolean listed;
    private int latency;
    private GameMode gameMode;
    private @Nullable Component displayName;
    private boolean showHat;
    private int listOrder;
    private UUID sessionId;
    private long expiresAt;
    private @Nullable PublicKey publicKey;
    private byte @Nullable [] keySignature;

    public PlayerListEntry(UUID profileId) {
        this(profileId, null, false, 0, GameMode.SURVIVAL, null, false, 0, null, 0, null, null);
    }
}
