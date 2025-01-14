package org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.WobbleStyle;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.BellValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.BellValueType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.BlockValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.BlockValueType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.ChestValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.ChestValueType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.DecoratedPotValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.DecoratedPotValueType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.EndGatewayValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.EndGatewayValueType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.GenericBlockValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.GenericBlockValueType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.MobSpawnerValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.MobSpawnerValueType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.NoteBlockValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.NoteBlockValueType;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@With
@AllArgsConstructor
public class ClientboundBlockEventPacket implements MinecraftPacket {
    // Do we really want these hardcoded values?
    private static final int NOTE_BLOCK = 109;
    private static final int STICKY_PISTON = 128;
    private static final int PISTON = 135;
    private static final int MOB_SPAWNER = 182;
    private static final int CHEST = 185;
    private static final int ENDER_CHEST = 365;
    private static final int TRAPPED_CHEST = 434;
    private static final int END_GATEWAY = 631;
    private static final int SHULKER_BOX_LOWER = 641;
    private static final int SHULKER_BOX_HIGHER = 657;
    private static final int BELL = 811;
    private static final int DECORATED_POT = 1083;
    private static final Logger log = LoggerFactory.getLogger(ClientboundBlockEventPacket.class);

    private final @NonNull Vector3i position;
    private final int rawType;
    private final int rawValue;
    private @Nullable BlockValueType type;
    private @Nullable BlockValue value;
    private final int blockId;

    public ClientboundBlockEventPacket(ByteBuf in, MinecraftCodecHelper helper) {
        this.position = helper.readPosition(in);
        this.rawType = in.readUnsignedByte();
        this.rawValue = in.readUnsignedByte();
        this.blockId = helper.readVarInt(in);

        // TODO: Handle this in MinecraftCodecHelper
        try {
            if (this.blockId == NOTE_BLOCK) {
                this.type = NoteBlockValueType.from(rawType);
                this.value = new NoteBlockValue();
            } else if (this.blockId == STICKY_PISTON || this.blockId == PISTON) {
                this.type = PistonValueType.from(rawType);
                this.value = new PistonValue(Direction.from(Math.abs((rawValue & 7) % 6)));
            } else if (this.blockId == MOB_SPAWNER) {
                this.type = MobSpawnerValueType.from(rawType - 1);
                this.value = new MobSpawnerValue();
            } else if (this.blockId == CHEST || this.blockId == ENDER_CHEST || this.blockId == TRAPPED_CHEST
                || (this.blockId >= SHULKER_BOX_LOWER && this.blockId <= SHULKER_BOX_HIGHER)) {
                this.type = ChestValueType.from(rawType - 1);
                this.value = new ChestValue(rawValue);
            } else if (this.blockId == END_GATEWAY) {
                this.type = EndGatewayValueType.from(rawType - 1);
                this.value = new EndGatewayValue();
            } else if (this.blockId == BELL) {
                this.type = BellValueType.from(rawType - 1);
                this.value = new BellValue(Direction.from(Math.abs(rawValue % 6)));
            } else if (this.blockId == DECORATED_POT) {
                this.type = DecoratedPotValueType.from(rawType - 1);
                this.value = new DecoratedPotValue(WobbleStyle.from(Math.abs(rawValue % 2)));
            } else {
                this.type = GenericBlockValueType.from(rawType);
                this.value = new GenericBlockValue(rawValue);
            }
        } catch (Throwable t) {
            this.type = null;
            this.value = null;
            log.warn("Unable to deserialize type and value! Message: {} (block: {}, type: {}, value: {})", t.getMessage(), blockId, rawType, rawValue);
        }
    }

    @Override
    public void serialize(ByteBuf out, MinecraftCodecHelper helper) {
        helper.writePosition(out, this.position);
        out.writeByte(rawType);
        out.writeByte(rawValue);
        helper.writeVarInt(out, this.blockId);
    }

    @Override
    public boolean shouldRunOnGameThread() {
        return true;
    }
}
