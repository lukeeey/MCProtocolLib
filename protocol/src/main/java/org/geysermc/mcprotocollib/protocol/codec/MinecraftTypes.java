package org.geysermc.mcprotocollib.protocol.codec;

import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import lombok.NoArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.math.vector.Vector4f;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.DefaultComponentSerializer;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatTypeDecoration;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.BlankFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.FixedFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.StyledFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.BitStorage;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.GlobalPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.ListPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.MapPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.Palette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.PaletteType;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.SingletonPalette;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.ArmadilloState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.PaintingVariant;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.SnifferState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.VillagerData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.WolfVariant;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.BlockBreakStage;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.geysermc.mcprotocollib.protocol.data.game.level.LightUpdateData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.LevelEventType;
import org.geysermc.mcprotocollib.protocol.data.game.level.event.UnknownLevelEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.BlockParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.DustColorTransitionParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.DustParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.EntityEffectParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ItemParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.SculkChargeParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ShriekParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.TrailParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.VibrationParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.BlockPositionSource;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.EntityPositionSource;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.PositionSource;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.PositionSourceType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.CustomSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.Sound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.SoundCategory;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.Ingredient;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.FurnaceRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplayType;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapedCraftingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapelessCraftingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.SmithingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.StonecutterRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.AnyFuelSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.CompositeSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.EmptySlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.RecipeSlotType;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SmithingTrimDemoSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.TagSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.WithRemainderSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.statistic.StatisticCategory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class MinecraftTypes {
    public static final NetworkCodec<Boolean> BOOLEAN = NetworkCodec.of(
        ByteBuf::writeBoolean,
        ByteBuf::readBoolean
    );
    public static final NetworkCodec<String> STRING = NetworkCodec.of(
        MinecraftTypes::writeString,
        MinecraftTypes::readString
    );
    public static final NetworkCodec<Integer> VAR_INT = NetworkCodec.of(
        MinecraftTypes::writeVarInt,
        MinecraftTypes::readVarInt
    );
    public static final NetworkCodec<Long> VAR_LONG = NetworkCodec.of(
        MinecraftTypes::writeVarLong,
        MinecraftTypes::readVarLong
    );
    public static final NetworkCodec<UUID> UUID = NetworkCodec.of(
        MinecraftTypes::writeUUID,
        MinecraftTypes::readUUID
    );
    public static final NetworkCodec<byte[]> BYTE_ARRAY = NetworkCodec.of(
        MinecraftTypes::writeByteArray,
        MinecraftTypes::readByteArray
    );
    public static final NetworkCodec<byte[]> GREEDY_BYTE_ARRAY = NetworkCodec.of(
        ByteBuf::writeBytes,
        buf -> MinecraftTypes.readByteArray(buf, ByteBuf::readableBytes)
    );
    public static final NetworkCodec<long[]> LONG_ARRAY = NetworkCodec.of(
        MinecraftTypes::writeLongArray,
        MinecraftTypes::readLongArray
    );
    public static final NetworkCodec<NbtMap> COMPOUND_TAG = NetworkCodec.of(
        MinecraftTypes::writeAnyTag,
        MinecraftTypes::readCompoundTag
    );
    public static final NetworkCodec<Component> COMPONENT = NetworkCodec.of(
        MinecraftTypes::writeComponent,
        MinecraftTypes::readComponent
    );
    public static final NetworkCodec<PublicKey> PUBLIC_KEY = BYTE_ARRAY.map(key -> {
        try {
            return key.getEncoded();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not encode public key", e);
        }
    }, bytes -> {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not decode public key", e);
        }
    });
    private static final int POSITION_X_SIZE = 38;
    private static final int POSITION_Y_SIZE = 12;
    private static final int POSITION_Z_SIZE = 38;
    private static final int POSITION_Y_SHIFT = 0xFFF;
    private static final int POSITION_WRITE_SHIFT = 0x3FFFFFF;

    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & ~0x7F) != 0) {
            buf.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }

        buf.writeByte(value);
    }

    public static int readVarInt(ByteBuf buf) {
        int value = 0;
        int size = 0;

        byte b;
        do {
            if (size >= 35) {
                throw new RuntimeException("VarInt wider than 5 bytes");
            }
            b = buf.readByte();
            value |= (b & 0x7F) << size;
            size += 7;
        } while ((b & 0x80) == 0x80);

        return value;
    }

    public static void writeVarLong(ByteBuf buf, long value) {
        while ((value & ~0x7FL) != 0) {
            buf.writeByte((int) (value & 0x7F | 0x80));
            value >>>= 7;
        }

        buf.writeByte((int) value);
    }

    public static long readVarLong(ByteBuf buf) {
        long value = 0;
        int size = 0;

        byte b;
        do {
            if (size >= 70) {
                throw new RuntimeException("VarLong wider than 10 bytes");
            }
            b = buf.readByte();
            value |= (b & 0x7FL) << size;
            size += 7;
        } while ((b & 0x80) == 0x80);

        return value;
    }

    public static String readString(ByteBuf buf) {
        return MinecraftTypes.readString(buf, Short.MAX_VALUE);
    }

    public static String readString(ByteBuf buf, int maxLength) {
        int length = MinecraftTypes.readVarInt(buf);
        if (length > maxLength * 3) {
            throw new IllegalArgumentException("String buffer is longer than maximum allowed length");
        }
        String string = (String) buf.readCharSequence(length, StandardCharsets.UTF_8);
        if (string.length() > maxLength) {
            throw new IllegalArgumentException("String is longer than maximum allowed length");
        }

        return string;
    }

    public static void writeString(ByteBuf buf, String value) {
        MinecraftTypes.writeVarInt(buf, ByteBufUtil.utf8Bytes(value));
        buf.writeCharSequence(value, StandardCharsets.UTF_8);
    }

    @Nullable
    public static <T> T readNullable(ByteBuf buf, Function<ByteBuf, T> ifPresent) {
        if (buf.readBoolean()) {
            return ifPresent.apply(buf);
        } else {
            return null;
        }
    }

    public static <T> void writeNullable(ByteBuf buf, @Nullable T value, BiConsumer<ByteBuf, T> ifPresent) {
        if (value != null) {
            buf.writeBoolean(true);
            ifPresent.accept(buf, value);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static <T> List<T> readList(ByteBuf buf, Function<ByteBuf, T> reader) {
        int size = MinecraftTypes.readVarInt(buf);
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(reader.apply(buf));
        }

        return list;
    }

    public static <T> void writeList(ByteBuf buf, List<T> value, BiConsumer<ByteBuf, T> writer) {
        MinecraftTypes.writeVarInt(buf, value.size());
        for (T t : value) {
            writer.accept(buf, t);
        }
    }

    public static <T> Holder<T> readHolder(ByteBuf buf, Function<ByteBuf, T> readCustom) {
        int registryId = MinecraftTypes.readVarInt(buf);
        return registryId == 0 ? Holder.ofCustom(readCustom.apply(buf)) : Holder.ofId(registryId - 1);
    }

    public static <T> void writeHolder(ByteBuf buf, Holder<T> holder, BiConsumer<ByteBuf, T> writeCustom) {
        if (holder.isCustom()) {
            MinecraftTypes.writeVarInt(buf, 0);
            writeCustom.accept(buf, holder.custom());
        } else {
            MinecraftTypes.writeVarInt(buf, holder.id() + 1);
        }
    }

    public static HolderSet readHolderSet(ByteBuf buf) {
        int length = MinecraftTypes.readVarInt(buf) - 1;
        if (length == -1) {
            return new HolderSet(MinecraftTypes.readResourceLocation(buf));
        } else {
            int[] holders = new int[length];
            for (int i = 0; i < length; i++) {
                holders[i] = MinecraftTypes.readVarInt(buf);
            }

            return new HolderSet(holders);
        }
    }

    public static void writeHolderSet(ByteBuf buf, HolderSet holderSet) {
        if (holderSet.getLocation() != null) {
            MinecraftTypes.writeVarInt(buf, 0);
            MinecraftTypes.writeResourceLocation(buf, holderSet.getLocation());
        } else {
            assert holderSet.getHolders() != null;
            MinecraftTypes.writeVarInt(buf, holderSet.getHolders().length + 1);
            for (int holder : holderSet.getHolders()) {
                MinecraftTypes.writeVarInt(buf, holder);
            }
        }
    }

    @SuppressWarnings("PatternValidation")
    public static Key readResourceLocation(ByteBuf buf) {
        return Key.key(MinecraftTypes.readString(buf));
    }

    public static void writeResourceLocation(ByteBuf buf, Key location) {
        MinecraftTypes.writeString(buf, location.asString());
    }

    public static UUID readUUID(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static byte[] readByteArray(ByteBuf buf) {
        return MinecraftTypes.readByteArray(buf, MinecraftTypes::readVarInt);
    }

    public static byte[] readByteArray(ByteBuf buf, ToIntFunction<ByteBuf> reader) {
        int length = reader.applyAsInt(buf);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }

    public static void writeByteArray(ByteBuf buf, byte[] bytes) {
        MinecraftTypes.writeByteArray(buf, bytes, MinecraftTypes::writeVarInt);
    }

    public static void writeByteArray(ByteBuf buf, byte[] bytes, ObjIntConsumer<ByteBuf> writer) {
        writer.accept(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static long[] readLongArray(ByteBuf buf) {
        return MinecraftTypes.readLongArray(buf, MinecraftTypes::readVarInt);
    }

    public static long[] readLongArray(ByteBuf buf, ToIntFunction<ByteBuf> reader) {
        int length = reader.applyAsInt(buf);
        if (length < 0) {
            throw new IllegalArgumentException("Array cannot have length less than 0.");
        }

        long[] l = new long[length];
        for (int index = 0; index < length; index++) {
            l[index] = buf.readLong();
        }

        return l;
    }

    public static void writeLongArray(ByteBuf buf, long[] l) {
        MinecraftTypes.writeLongArray(buf, l, MinecraftTypes::writeVarInt);
    }

    public static void writeLongArray(ByteBuf buf, long[] l, ObjIntConsumer<ByteBuf> writer) {
        writer.accept(buf, l.length);
        for (long value : l) {
            buf.writeLong(value);
        }
    }

    @Nullable
    public static NbtMap readCompoundTag(ByteBuf buf) {
        return readAnyTag(buf, NbtType.COMPOUND);
    }

    @NonNull
    public static NbtMap readCompoundTagOrThrow(ByteBuf buf) {
        NbtMap tag = readCompoundTag(buf);
        if (tag == null) {
            throw new IllegalArgumentException("Got end-tag when trying to read NbtMap");
        }
        return tag;
    }

    @Nullable
    public static <T> T readAnyTag(ByteBuf buf, NbtType<T> expected) {
        Object tag = MinecraftTypes.readAnyTag(buf);

        if (tag == null) {
            return null;
        }

        Class<T> tagClass = expected.getTagClass();
        if (!tagClass.isInstance(tag)) {
            throw new IllegalArgumentException("Expected tag of type " + tagClass.getName() + " but got " + tag.getClass().getName());
        }

        return tagClass.cast(tag);
    }

    @Nullable
    public static Object readAnyTag(ByteBuf buf) {
        try {
            ByteBufInputStream input = new ByteBufInputStream(buf);

            int typeId = input.readUnsignedByte();
            if (typeId == 0) {
                return null;
            }

            NbtType<?> type = NbtType.byId(typeId);

            return new NBTInputStream(input).readValue(type, 512);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void writeAnyTag(ByteBuf buf, @Nullable Object tag) {
        try {
            ByteBufOutputStream output = new ByteBufOutputStream(buf);

            if (tag == null) {
                output.writeByte(0);
                return;
            }

            NbtType<?> type = NbtType.byClass(tag.getClass());
            output.writeByte(type.getId());

            new NBTOutputStream(output).writeValue(tag, 512);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Nullable
    public static ItemStack readOptionalItemStack(ByteBuf buf) {
        int count = MinecraftTypes.readVarInt(buf);
        if (count <= 0) {
            return null;
        }

        int item = MinecraftTypes.readVarInt(buf);
        return new ItemStack(item, count, MinecraftTypes.readDataComponentPatch(buf));
    }

    public static void writeOptionalItemStack(ByteBuf buf, ItemStack item) {
        boolean empty = item == null || item.getAmount() <= 0;
        MinecraftTypes.writeVarInt(buf, !empty ? item.getAmount() : 0);
        if (!empty) {
            MinecraftTypes.writeVarInt(buf, item.getId());
            MinecraftTypes.writeDataComponentPatch(buf, item.getDataComponents());
        }
    }

    @NotNull
    public static ItemStack readItemStack(ByteBuf buf) {
        return MinecraftTypes.readOptionalItemStack(buf);
    }

    public static void writeItemStack(ByteBuf buf, @NotNull ItemStack item) {
        MinecraftTypes.writeOptionalItemStack(buf, item);
    }

    @Nullable
    public static DataComponents readDataComponentPatch(ByteBuf buf) {
        int nonNullComponents = MinecraftTypes.readVarInt(buf);
        int nullComponents = MinecraftTypes.readVarInt(buf);
        if (nonNullComponents == 0 && nullComponents == 0) {
            return null;
        }

        Map<DataComponentType<?>, DataComponent<?, ?>> dataComponents = new HashMap<>();
        for (int k = 0; k < nonNullComponents; k++) {
            DataComponentType<?> dataComponentType = DataComponentType.from(MinecraftTypes.readVarInt(buf));
            DataComponent<?, ?> dataComponent = dataComponentType.readDataComponent(buf);
            dataComponents.put(dataComponentType, dataComponent);
        }

        for (int k = 0; k < nullComponents; k++) {
            DataComponentType<?> dataComponentType = DataComponentType.from(MinecraftTypes.readVarInt(buf));
            DataComponent<?, ?> dataComponent = dataComponentType.readNullDataComponent();
            dataComponents.put(dataComponentType, dataComponent);
        }

        return new DataComponents(dataComponents);
    }

    public static void writeDataComponentPatch(ByteBuf buf, DataComponents dataComponents) {
        if (dataComponents == null) {
            MinecraftTypes.writeVarInt(buf, 0);
            MinecraftTypes.writeVarInt(buf, 0);
        } else {
            int i = 0;
            int j = 0;
            for (DataComponent<?, ?> component : dataComponents.getDataComponents().values()) {
                if (component.getValue() != null) {
                    i++;
                } else {
                    j++;
                }
            }

            MinecraftTypes.writeVarInt(buf, i);
            MinecraftTypes.writeVarInt(buf, j);

            for (DataComponent<?, ?> component : dataComponents.getDataComponents().values()) {
                if (component.getValue() != null) {
                    MinecraftTypes.writeVarInt(buf, component.getType().getId());
                    component.write(buf);
                }
            }

            for (DataComponent<?, ?> component : dataComponents.getDataComponents().values()) {
                if (component.getValue() == null) {
                    MinecraftTypes.writeVarInt(buf, component.getType().getId());
                }
            }
        }
    }

    @NotNull
    public static ItemStack readTradeItemStack(ByteBuf buf) {
        int item = MinecraftTypes.readVarInt(buf);
        int count = MinecraftTypes.readVarInt(buf);
        int componentsLength = MinecraftTypes.readVarInt(buf);

        Map<DataComponentType<?>, DataComponent<?, ?>> dataComponents = new HashMap<>();
        for (int i = 0; i < componentsLength; i++) {
            DataComponentType<?> dataComponentType = DataComponentType.from(MinecraftTypes.readVarInt(buf));
            DataComponent<?, ?> dataComponent = dataComponentType.readDataComponent(buf);
            dataComponents.put(dataComponentType, dataComponent);
        }

        return new ItemStack(item, count, new DataComponents(dataComponents));
    }

    public static void writeTradeItemStack(ByteBuf buf, @NotNull ItemStack item) {
        MinecraftTypes.writeVarInt(buf, item.getId());
        MinecraftTypes.writeVarInt(buf, item.getAmount());

        DataComponents dataComponents = item.getDataComponents();
        if (dataComponents == null) {
            MinecraftTypes.writeVarInt(buf, 0);
            return;
        }

        MinecraftTypes.writeVarInt(buf, dataComponents.getDataComponents().size());
        for (DataComponent<?, ?> component : dataComponents.getDataComponents().values()) {
            MinecraftTypes.writeVarInt(buf, component.getType().getId());
            component.write(buf);
        }
    }

    public static Vector3i readPosition(ByteBuf buf) {
        long val = buf.readLong();

        int x = (int) (val >> POSITION_X_SIZE);
        int y = (int) (val << 52 >> 52);
        int z = (int) (val << 26 >> POSITION_Z_SIZE);

        return Vector3i.from(x, y, z);
    }

    public static void writePosition(ByteBuf buf, Vector3i pos) {
        long x = pos.getX() & POSITION_WRITE_SHIFT;
        long y = pos.getY() & POSITION_Y_SHIFT;
        long z = pos.getZ() & POSITION_WRITE_SHIFT;

        buf.writeLong(x << POSITION_X_SIZE | z << POSITION_Y_SIZE | y);
    }

    public static Vector3f readRotation(ByteBuf buf) {
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();

        return Vector3f.from(x, y, z);
    }

    public static void writeRotation(ByteBuf buf, Vector3f rot) {
        buf.writeFloat(rot.getX());
        buf.writeFloat(rot.getY());
        buf.writeFloat(rot.getZ());
    }

    public static Vector4f readQuaternion(ByteBuf buf) {
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        float w = buf.readFloat();

        return Vector4f.from(x, y, z, w);
    }

    public static void writeQuaternion(ByteBuf buf, Vector4f vec4) {
        buf.writeFloat(vec4.getX());
        buf.writeFloat(vec4.getY());
        buf.writeFloat(vec4.getZ());
        buf.writeFloat(vec4.getW());
    }

    public static Direction readDirection(ByteBuf buf) {
        return Direction.from(MinecraftTypes.readVarInt(buf));
    }

    public static void writeDirection(ByteBuf buf, Direction dir) {
        MinecraftTypes.writeEnum(buf, dir);
    }

    public static Pose readPose(ByteBuf buf) {
        return Pose.from(MinecraftTypes.readVarInt(buf));
    }

    public static void writePose(ByteBuf buf, Pose pose) {
        MinecraftTypes.writeEnum(buf, pose);
    }

    public static Holder<WolfVariant> readWolfVariant(ByteBuf buf) {
        return MinecraftTypes.readHolder(buf, input -> {
            Key wildTexture = MinecraftTypes.readResourceLocation(input);
            Key tameTexture = MinecraftTypes.readResourceLocation(input);
            Key angryTexture = MinecraftTypes.readResourceLocation(input);
            Key biomeLocation = null;
            int[] biomeHolders = null;

            int length = MinecraftTypes.readVarInt(input) - 1;
            if (length == -1) {
                biomeLocation = MinecraftTypes.readResourceLocation(input);
            } else {
                biomeHolders = new int[length];
                for (int j = 0; j < length; j++) {
                    biomeHolders[j] = MinecraftTypes.readVarInt(input);
                }
            }
            return new WolfVariant(wildTexture, tameTexture, angryTexture, biomeLocation, biomeHolders);
        });
    }

    public static void writeWolfVariant(ByteBuf buf, Holder<WolfVariant> variantHolder) {
        MinecraftTypes.writeHolder(buf, variantHolder, (output, variant) -> {
            MinecraftTypes.writeResourceLocation(output, variant.wildTexture());
            MinecraftTypes.writeResourceLocation(output, variant.tameTexture());
            MinecraftTypes.writeResourceLocation(output, variant.angryTexture());
            if (variant.biomeLocation() != null) {
                MinecraftTypes.writeVarInt(output, 0);
                MinecraftTypes.writeResourceLocation(output, variant.biomeLocation());
            } else {
                MinecraftTypes.writeVarInt(output, variant.biomeHolders().length + 1);
                for (int holder : variant.biomeHolders()) {
                    MinecraftTypes.writeVarInt(output, holder);
                }
            }
        });
    }

    public static Holder<PaintingVariant> readPaintingVariant(ByteBuf buf) {
        return MinecraftTypes.readHolder(buf, input -> {
            return new PaintingVariant(MinecraftTypes.readVarInt(input), MinecraftTypes.readVarInt(input), MinecraftTypes.readResourceLocation(input),
                    MinecraftTypes.readNullable(input, MinecraftTypes::readComponent), MinecraftTypes.readNullable(input, MinecraftTypes::readComponent));
        });
    }

    public static void writePaintingVariant(ByteBuf buf, Holder<PaintingVariant> variantHolder) {
        MinecraftTypes.writeHolder(buf, variantHolder, (output, variant) -> {
            MinecraftTypes.writeVarInt(buf, variant.width());
            MinecraftTypes.writeVarInt(buf, variant.height());
            MinecraftTypes.writeResourceLocation(buf, variant.assetId());
            MinecraftTypes.writeNullable(buf, variant.title(), MinecraftTypes::writeComponent);
            MinecraftTypes.writeNullable(buf, variant.author(), MinecraftTypes::writeComponent);
        });
    }

    public static SnifferState readSnifferState(ByteBuf buf) {
        return SnifferState.from(MinecraftTypes.readVarInt(buf));
    }

    public static void writeSnifferState(ByteBuf buf, SnifferState state) {
        MinecraftTypes.writeEnum(buf, state);
    }

    public static ArmadilloState readArmadilloState(ByteBuf buf) {
        return ArmadilloState.from(MinecraftTypes.readVarInt(buf));
    }

    public static void writeArmadilloState(ByteBuf buf, ArmadilloState state) {
        MinecraftTypes.writeEnum(buf, state);
    }

    private static void writeEnum(ByteBuf buf, Enum<?> e) {
        MinecraftTypes.writeVarInt(buf, e.ordinal());
    }

    public static Component readComponent(ByteBuf buf) {
        // do not use NbtMap, as mojang serializes a plaintext component as just a single StringTag
        Object tag = readAnyTag(buf);
        if (tag == null) {
            throw new IllegalArgumentException("Got end-tag when trying to read Component");
        }
        JsonElement json = NbtComponentSerializer.tagComponentToJson(tag);
        return DefaultComponentSerializer.get().deserializeFromTree(json);
    }

    public static void writeComponent(ByteBuf buf, Component component) {
        JsonElement json = DefaultComponentSerializer.get().serializeToTree(component);
        Object tag = NbtComponentSerializer.jsonComponentToTag(json);
        writeAnyTag(buf, tag);
    }

    public static EntityMetadata<?, ?>[] readEntityMetadata(ByteBuf buf) {
        List<EntityMetadata<?, ?>> ret = new ArrayList<>();
        int id;
        while ((id = buf.readUnsignedByte()) != 255) {
            ret.add(MinecraftTypes.readMetadata(buf, id));
        }

        return ret.toArray(new EntityMetadata<?, ?>[0]);
    }

    public static void writeEntityMetadata(ByteBuf buf, EntityMetadata<?, ?>[] metadata) {
        for (EntityMetadata<?, ?> meta : metadata) {
            MinecraftTypes.writeMetadata(buf, meta);
        }

        buf.writeByte(255);
    }

    public static EntityMetadata<?, ?> readMetadata(ByteBuf buf, int id) {
        MetadataType<?> type = MinecraftTypes.readMetadataType(buf);
        return type.readMetadata(buf, id);
    }

    public static void writeMetadata(ByteBuf buf, EntityMetadata<?, ?> metadata) {
        buf.writeByte(metadata.getId());
        MinecraftTypes.writeMetadataType(buf, metadata.getType());
        metadata.write(buf);
    }

    public static MetadataType<?> readMetadataType(ByteBuf buf) {
        int id = MinecraftTypes.readVarInt(buf);
        if (id >= MetadataType.size()) {
            throw new IllegalArgumentException("Received id " + id + " for MetadataType when the maximum was " + MetadataType.size() + "!");
        }

        return MetadataType.from(id);
    }

    public static void writeMetadataType(ByteBuf buf, MetadataType<?> type) {
        MinecraftTypes.writeVarInt(buf, type.getId());
    }

    public static GlobalPos readGlobalPos(ByteBuf buf) {
        Key dimension = readResourceLocation(buf);
        Vector3i pos = MinecraftTypes.readPosition(buf);
        return new GlobalPos(dimension, pos);
    }

    public static void writeGlobalPos(ByteBuf buf, GlobalPos pos) {
        MinecraftTypes.writeResourceLocation(buf, pos.getDimension());
        MinecraftTypes.writePosition(buf, pos.getPosition());
    }

    public static PlayerSpawnInfo readPlayerSpawnInfo(ByteBuf buf) {
        int dimension = MinecraftTypes.readVarInt(buf);
        Key worldName = MinecraftTypes.readResourceLocation(buf);
        long hashedSeed = buf.readLong();
        GameMode gameMode = GameMode.byId(buf.readByte());
        GameMode previousGamemode = GameMode.byNullableId(buf.readByte());
        boolean debug = buf.readBoolean();
        boolean flat = buf.readBoolean();
        GlobalPos lastDeathPos = MinecraftTypes.readNullable(buf, MinecraftTypes::readGlobalPos);
        int portalCooldown = MinecraftTypes.readVarInt(buf);
        int seaLevel = MinecraftTypes.readVarInt(buf);
        return new PlayerSpawnInfo(dimension, worldName, hashedSeed, gameMode, previousGamemode, debug, flat, lastDeathPos, portalCooldown, seaLevel);
    }

    public static void writePlayerSpawnInfo(ByteBuf buf, PlayerSpawnInfo info) {
        MinecraftTypes.writeVarInt(buf, info.getDimension());
        MinecraftTypes.writeResourceLocation(buf, info.getWorldName());
        buf.writeLong(info.getHashedSeed());
        buf.writeByte(info.getGameMode().ordinal());
        buf.writeByte(GameMode.toNullableId(info.getPreviousGamemode()));
        buf.writeBoolean(info.isDebug());
        buf.writeBoolean(info.isFlat());
        MinecraftTypes.writeNullable(buf, info.getLastDeathPos(), MinecraftTypes::writeGlobalPos);
        MinecraftTypes.writeVarInt(buf, info.getPortalCooldown());
        MinecraftTypes.writeVarInt(buf, info.getSeaLevel());
    }

    public static ParticleType readParticleType(ByteBuf buf) {
        return ParticleType.from(MinecraftTypes.readVarInt(buf));
    }

    public static void writeParticleType(ByteBuf buf, ParticleType type) {
        MinecraftTypes.writeEnum(buf, type);
    }

    public static Particle readParticle(ByteBuf buf) {
        ParticleType particleType = MinecraftTypes.readParticleType(buf);
        return new Particle(particleType, MinecraftTypes.readParticleData(buf, particleType));
    }

    public static void writeParticle(ByteBuf buf, Particle particle) {
        MinecraftTypes.writeEnum(buf, particle.getType());
        MinecraftTypes.writeParticleData(buf, particle.getType(), particle.getData());
    }

    public static ParticleData readParticleData(ByteBuf buf, ParticleType type) {
        return switch (type) {
            case BLOCK, BLOCK_MARKER, FALLING_DUST, DUST_PILLAR, BLOCK_CRUMBLE -> new BlockParticleData(MinecraftTypes.readVarInt(buf));
            case DUST -> {
                int color = buf.readInt();
                float scale = buf.readFloat();
                yield new DustParticleData(color, scale);
            }
            case DUST_COLOR_TRANSITION -> {
                int color = buf.readInt();
                int newColor = buf.readInt();
                float scale = buf.readFloat();
                yield new DustColorTransitionParticleData(color, scale, newColor);
            }
            case ENTITY_EFFECT -> new EntityEffectParticleData(buf.readInt());
            case ITEM -> new ItemParticleData(MinecraftTypes.readOptionalItemStack(buf));
            case SCULK_CHARGE -> new SculkChargeParticleData(buf.readFloat());
            case SHRIEK -> new ShriekParticleData(MinecraftTypes.readVarInt(buf));
            case TRAIL -> new TrailParticleData(Vector3d.from(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readInt(), MinecraftTypes.readVarInt(buf));
            case VIBRATION -> new VibrationParticleData(MinecraftTypes.readPositionSource(buf), MinecraftTypes.readVarInt(buf));
            default -> null;
        };
    }

    public static void writeParticleData(ByteBuf buf, ParticleType type, ParticleData data) {
        switch (type) {
            case BLOCK, BLOCK_MARKER, FALLING_DUST, DUST_PILLAR, BLOCK_CRUMBLE -> {
                BlockParticleData blockData = (BlockParticleData) data;
                MinecraftTypes.writeVarInt(buf, blockData.getBlockState());
            }
            case DUST -> {
                DustParticleData dustData = (DustParticleData) data;
                buf.writeInt(dustData.getColor());
                buf.writeFloat(dustData.getScale());
            }
            case DUST_COLOR_TRANSITION -> {
                DustColorTransitionParticleData dustData = (DustColorTransitionParticleData) data;
                buf.writeInt(dustData.getColor());
                buf.writeInt(dustData.getNewColor());
                buf.writeFloat(dustData.getScale());
            }
            case ENTITY_EFFECT -> {
                EntityEffectParticleData entityEffectData = (EntityEffectParticleData) data;
                buf.writeInt(entityEffectData.getColor());
            }
            case ITEM -> {
                ItemParticleData itemData = (ItemParticleData) data;
                MinecraftTypes.writeOptionalItemStack(buf, itemData.getItemStack());
            }
            case SCULK_CHARGE -> {
                SculkChargeParticleData sculkData = (SculkChargeParticleData) data;
                buf.writeFloat(sculkData.getRoll());
            }
            case SHRIEK -> {
                ShriekParticleData shriekData = (ShriekParticleData) data;
                MinecraftTypes.writeVarInt(buf, shriekData.getDelay());
            }
            case TRAIL -> {
                TrailParticleData trailData = (TrailParticleData) data;
                buf.writeDouble(trailData.target().getX());
                buf.writeDouble(trailData.target().getY());
                buf.writeDouble(trailData.target().getZ());
                buf.writeInt(trailData.color());
                MinecraftTypes.writeVarInt(buf, trailData.duration());
            }
            case VIBRATION -> {
                VibrationParticleData vibrationData = (VibrationParticleData) data;
                MinecraftTypes.writePositionSource(buf, vibrationData.getPositionSource());
                MinecraftTypes.writeVarInt(buf, vibrationData.getArrivalTicks());
            }
        }
    }

    public static NumberFormat readNumberFormat(ByteBuf buf) {
        int id = MinecraftTypes.readVarInt(buf);
        return switch (id) {
            case 0 -> BlankFormat.INSTANCE;
            case 1 -> new StyledFormat(MinecraftTypes.readCompoundTagOrThrow(buf));
            case 2 -> new FixedFormat(MinecraftTypes.readComponent(buf));
            default -> throw new IllegalArgumentException("Unknown number format type: " + id);
        };
    }

    public static void writeNumberFormat(ByteBuf buf, NumberFormat numberFormat) {
        if (numberFormat instanceof BlankFormat) {
            MinecraftTypes.writeVarInt(buf, 0);
        } else if (numberFormat instanceof StyledFormat styledFormat) {
            MinecraftTypes.writeVarInt(buf, 1);
            MinecraftTypes.writeAnyTag(buf, styledFormat.getStyle());
        } else if (numberFormat instanceof FixedFormat fixedFormat) {
            MinecraftTypes.writeVarInt(buf, 2);
            MinecraftTypes.writeComponent(buf, fixedFormat.getValue());
        } else {
            throw new IllegalArgumentException("Unknown number format: " + numberFormat);
        }
    }

    public static ChatType readChatType(ByteBuf buf) {
        return new ChatType(readChatTypeDecoration(buf), readChatTypeDecoration(buf));
    }

    public static void writeChatType(ByteBuf buf, ChatType chatType) {
        MinecraftTypes.writeChatTypeDecoration(buf, chatType.chat());
        MinecraftTypes.writeChatTypeDecoration(buf, chatType.narration());
    }

    public static ChatTypeDecoration readChatTypeDecoration(ByteBuf buf) {
        String translationKey = MinecraftTypes.readString(buf);

        int size = MinecraftTypes.readVarInt(buf);
        List<ChatTypeDecoration.Parameter> parameters = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            parameters.add(ChatTypeDecoration.Parameter.VALUES[MinecraftTypes.readVarInt(buf)]);
        }

        NbtMap style = MinecraftTypes.readCompoundTag(buf);
        return new ChatType.ChatTypeDecorationImpl(translationKey, parameters, style);
    }

    public static void writeChatTypeDecoration(ByteBuf buf, ChatTypeDecoration decoration) {
        MinecraftTypes.writeString(buf, decoration.translationKey());

        MinecraftTypes.writeVarInt(buf, decoration.parameters().size());
        for (ChatTypeDecoration.Parameter parameter : decoration.parameters()) {
            MinecraftTypes.writeVarInt(buf, parameter.ordinal());
        }

        MinecraftTypes.writeAnyTag(buf, decoration.style());
    }

    public static PositionSource readPositionSource(ByteBuf buf) {
        PositionSourceType type = PositionSourceType.from(MinecraftTypes.readVarInt(buf));
        return switch (type) {
            case BLOCK -> new BlockPositionSource(MinecraftTypes.readPosition(buf));
            case ENTITY -> new EntityPositionSource(MinecraftTypes.readVarInt(buf), buf.readFloat());
        };
    }

    public static void writePositionSource(ByteBuf buf, PositionSource positionSource) {
        MinecraftTypes.writeVarInt(buf, positionSource.getType().ordinal());
        if (positionSource instanceof BlockPositionSource blockPositionSource) {
            MinecraftTypes.writePosition(buf, blockPositionSource.getPosition());
        } else if (positionSource instanceof EntityPositionSource entityPositionSource) {
            MinecraftTypes.writeVarInt(buf, entityPositionSource.getEntityId());
            buf.writeFloat(entityPositionSource.getYOffset());
        } else {
            throw new IllegalStateException("Unknown position source type!");
        }
    }

    public static VillagerData readVillagerData(ByteBuf buf) {
        return new VillagerData(MinecraftTypes.readVarInt(buf), MinecraftTypes.readVarInt(buf), MinecraftTypes.readVarInt(buf));
    }

    public static void writeVillagerData(ByteBuf buf, VillagerData villagerData) {
        MinecraftTypes.writeVarInt(buf, villagerData.getType());
        MinecraftTypes.writeVarInt(buf, villagerData.getProfession());
        MinecraftTypes.writeVarInt(buf, villagerData.getLevel());
    }

    public static ModifierOperation readModifierOperation(ByteBuf buf) {
        return ModifierOperation.from(buf.readByte());
    }

    public static void writeModifierOperation(ByteBuf buf, ModifierOperation operation) {
        buf.writeByte(operation.ordinal());
    }

    public static Effect readEffect(ByteBuf buf) {
        return Effect.from(MinecraftTypes.readVarInt(buf));
    }

    public static void writeEffect(ByteBuf buf, Effect effect) {
        MinecraftTypes.writeVarInt(buf, effect.ordinal());
    }

    public static BlockBreakStage readBlockBreakStage(ByteBuf buf) {
        int stage = buf.readUnsignedByte();
        if (stage >= 0 && stage < 10) {
            return BlockBreakStage.STAGES[stage];
        } else {
            return BlockBreakStage.RESET;
        }
    }

    public static void writeBlockBreakStage(ByteBuf buf, BlockBreakStage stage) {
        if (stage == BlockBreakStage.RESET) {
            buf.writeByte(255);
        } else {
            buf.writeByte(stage.ordinal());
        }
    }

    @Nullable
    public static BlockEntityType readBlockEntityType(ByteBuf buf) {
        return BlockEntityType.from(MinecraftTypes.readVarInt(buf));
    }

    public static void writeBlockEntityType(ByteBuf buf, BlockEntityType type) {
        MinecraftTypes.writeEnum(buf, type);
    }

    public static LightUpdateData readLightUpdateData(ByteBuf buf) {
        BitSet skyYMask = BitSet.valueOf(MinecraftTypes.readLongArray(buf));
        BitSet blockYMask = BitSet.valueOf(MinecraftTypes.readLongArray(buf));
        BitSet emptySkyYMask = BitSet.valueOf(MinecraftTypes.readLongArray(buf));
        BitSet emptyBlockYMask = BitSet.valueOf(MinecraftTypes.readLongArray(buf));

        int skyUpdateSize = MinecraftTypes.readVarInt(buf);
        List<byte[]> skyUpdates = new ArrayList<>(skyUpdateSize);
        for (int i = 0; i < skyUpdateSize; i++) {
            skyUpdates.add(MinecraftTypes.readByteArray(buf));
        }

        int blockUpdateSize = MinecraftTypes.readVarInt(buf);
        List<byte[]> blockUpdates = new ArrayList<>(blockUpdateSize);
        for (int i = 0; i < blockUpdateSize; i++) {
            blockUpdates.add(MinecraftTypes.readByteArray(buf));
        }

        return new LightUpdateData(skyYMask, blockYMask, emptySkyYMask, emptyBlockYMask, skyUpdates, blockUpdates);
    }

    public static void writeLightUpdateData(ByteBuf buf, LightUpdateData data) {
        writeBitSet(buf, data.getSkyYMask());
        writeBitSet(buf, data.getBlockYMask());
        writeBitSet(buf, data.getEmptySkyYMask());
        writeBitSet(buf, data.getEmptyBlockYMask());

        MinecraftTypes.writeVarInt(buf, data.getSkyUpdates().size());
        for (byte[] array : data.getSkyUpdates()) {
            MinecraftTypes.writeByteArray(buf, array);
        }

        MinecraftTypes.writeVarInt(buf, data.getBlockUpdates().size());
        for (byte[] array : data.getBlockUpdates()) {
            MinecraftTypes.writeByteArray(buf, array);
        }
    }

    private static void writeBitSet(ByteBuf buf, BitSet bitSet) {
        long[] array = bitSet.toLongArray();
        MinecraftTypes.writeLongArray(buf, array);
    }

    public static LevelEvent readLevelEvent(ByteBuf buf) {
        int id = buf.readInt();
        LevelEventType type = LevelEventType.from(id);
        if (type != null) {
            return type;
        }
        return new UnknownLevelEvent(id);
    }

    public static void writeLevelEvent(ByteBuf buf, LevelEvent event) {
        buf.writeInt(event.getId());
    }

    public static StatisticCategory readStatisticCategory(ByteBuf buf) {
        return StatisticCategory.from(MinecraftTypes.readVarInt(buf));
    }

    public static void writeStatisticCategory(ByteBuf buf, StatisticCategory category) {
        MinecraftTypes.writeEnum(buf, category);
    }

    public static SoundCategory readSoundCategory(ByteBuf buf) {
        return SoundCategory.from(MinecraftTypes.readVarInt(buf));
    }

    public static void writeSoundCategory(ByteBuf buf, SoundCategory category) {
        MinecraftTypes.writeEnum(buf, category);
    }

    public static EntityEvent readEntityEvent(ByteBuf buf) {
        return EntityEvent.from(buf.readByte());
    }

    public static void writeEntityEvent(ByteBuf buf, EntityEvent event) {
        buf.writeByte(event.ordinal());
    }

    public static Ingredient readRecipeIngredient(ByteBuf buf) {
        return new Ingredient(MinecraftTypes.readHolderSet(buf));
    }

    public static void writeRecipeIngredient(ByteBuf buf, Ingredient ingredient) {
        MinecraftTypes.writeHolderSet(buf, ingredient.getValues());
    }

    public static RecipeDisplay readRecipeDisplay(ByteBuf buf) {
        RecipeDisplayType type = RecipeDisplayType.from(MinecraftTypes.readVarInt(buf));
        RecipeDisplay display;
        switch (type) {
            case CRAFTING_SHAPELESS -> {
                List<SlotDisplay> ingredients = MinecraftTypes.readList(buf, MinecraftTypes::readSlotDisplay);
                SlotDisplay result = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay craftingStation = MinecraftTypes.readSlotDisplay(buf);

                display = new ShapelessCraftingRecipeDisplay(ingredients, result, craftingStation);
            }
            case CRAFTING_SHAPED -> {
                int width = MinecraftTypes.readVarInt(buf);
                int height = MinecraftTypes.readVarInt(buf);
                List<SlotDisplay> ingredients = MinecraftTypes.readList(buf, MinecraftTypes::readSlotDisplay);
                SlotDisplay result = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay craftingStation = MinecraftTypes.readSlotDisplay(buf);

                display = new ShapedCraftingRecipeDisplay(width, height, ingredients, result, craftingStation);
            }
            case FURNACE -> {
                SlotDisplay ingredient = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay fuel = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay result = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay craftingStation = MinecraftTypes.readSlotDisplay(buf);
                int duration = MinecraftTypes.readVarInt(buf);
                float experience = buf.readFloat();

                display = new FurnaceRecipeDisplay(ingredient, fuel, result, craftingStation, duration, experience);
            }
            case STONECUTTER -> {
                SlotDisplay input = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay result = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay craftingStation = MinecraftTypes.readSlotDisplay(buf);

                display = new StonecutterRecipeDisplay(input, result, craftingStation);
            }
            case SMITHING -> {
                SlotDisplay template = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay base = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay addition = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay result = MinecraftTypes.readSlotDisplay(buf);
                SlotDisplay craftingStation = MinecraftTypes.readSlotDisplay(buf);

                display = new SmithingRecipeDisplay(template, base, addition, result, craftingStation);
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
        return display;
    }

    public static void writeRecipeDisplay(ByteBuf buf, RecipeDisplay display) {
        MinecraftTypes.writeVarInt(buf, display.getType().ordinal());
        switch (display.getType()) {
            case CRAFTING_SHAPELESS -> {
                ShapelessCraftingRecipeDisplay shapelessDisplay = (ShapelessCraftingRecipeDisplay) display;

                MinecraftTypes.writeList(buf, shapelessDisplay.ingredients(), MinecraftTypes::writeSlotDisplay);
                MinecraftTypes.writeSlotDisplay(buf, shapelessDisplay.result());
                MinecraftTypes.writeSlotDisplay(buf, shapelessDisplay.craftingStation());
            }
            case CRAFTING_SHAPED -> {
                ShapedCraftingRecipeDisplay shapedDisplay = (ShapedCraftingRecipeDisplay) display;

                MinecraftTypes.writeVarInt(buf, shapedDisplay.width());
                MinecraftTypes.writeVarInt(buf, shapedDisplay.height());
                MinecraftTypes.writeList(buf, shapedDisplay.ingredients(), MinecraftTypes::writeSlotDisplay);
                MinecraftTypes.writeSlotDisplay(buf, shapedDisplay.result());
                MinecraftTypes.writeSlotDisplay(buf, shapedDisplay.craftingStation());
            }
            case FURNACE -> {
                FurnaceRecipeDisplay furnaceDisplay = (FurnaceRecipeDisplay) display;

                MinecraftTypes.writeSlotDisplay(buf, furnaceDisplay.ingredient());
                MinecraftTypes.writeSlotDisplay(buf, furnaceDisplay.fuel());
                MinecraftTypes.writeSlotDisplay(buf, furnaceDisplay.result());
                MinecraftTypes.writeSlotDisplay(buf, furnaceDisplay.craftingStation());
                MinecraftTypes.writeVarInt(buf, furnaceDisplay.duration());
                buf.writeFloat(furnaceDisplay.experience());
            }
            case STONECUTTER -> {
                StonecutterRecipeDisplay stonecutterDisplay = (StonecutterRecipeDisplay) display;

                MinecraftTypes.writeSlotDisplay(buf, stonecutterDisplay.input());
                MinecraftTypes.writeSlotDisplay(buf, stonecutterDisplay.result());
                MinecraftTypes.writeSlotDisplay(buf, stonecutterDisplay.craftingStation());
            }
            case SMITHING -> {
                SmithingRecipeDisplay smithingDisplay = (SmithingRecipeDisplay) display;

                MinecraftTypes.writeSlotDisplay(buf, smithingDisplay.template());
                MinecraftTypes.writeSlotDisplay(buf, smithingDisplay.base());
                MinecraftTypes.writeSlotDisplay(buf, smithingDisplay.addition());
                MinecraftTypes.writeSlotDisplay(buf, smithingDisplay.result());
                MinecraftTypes.writeSlotDisplay(buf, smithingDisplay.craftingStation());
            }
        }
    }

    public static SlotDisplay readSlotDisplay(ByteBuf buf) {
        RecipeSlotType type = RecipeSlotType.from(MinecraftTypes.readVarInt(buf));
        SlotDisplay display;
        switch (type) {
            case EMPTY -> display = EmptySlotDisplay.INSTANCE;
            case ANY_FUEL -> display = new AnyFuelSlotDisplay();
            case ITEM -> display = new ItemSlotDisplay(MinecraftTypes.readVarInt(buf));
            case ITEM_STACK -> display = new ItemStackSlotDisplay(MinecraftTypes.readItemStack(buf));
            case TAG -> display = new TagSlotDisplay(MinecraftTypes.readResourceLocation(buf));
            case SMITHING_TRIM -> display = new SmithingTrimDemoSlotDisplay(MinecraftTypes.readSlotDisplay(buf), MinecraftTypes.readSlotDisplay(buf), MinecraftTypes.readSlotDisplay(buf));
            case WITH_REMAINDER -> display = new WithRemainderSlotDisplay(MinecraftTypes.readSlotDisplay(buf), MinecraftTypes.readSlotDisplay(buf));
            case COMPOSITE -> display = new CompositeSlotDisplay(MinecraftTypes.readList(buf, MinecraftTypes::readSlotDisplay));
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
        return display;
    }

    public static void writeSlotDisplay(ByteBuf buf, SlotDisplay display) {
        MinecraftTypes.writeVarInt(buf, display.getType().ordinal());
        switch (display.getType()) {
            case ITEM -> MinecraftTypes.writeVarInt(buf, ((ItemSlotDisplay)display).item());
            case ITEM_STACK -> MinecraftTypes.writeItemStack(buf, ((ItemStackSlotDisplay)display).itemStack());
            case TAG -> MinecraftTypes.writeResourceLocation(buf, ((TagSlotDisplay)display).tag());
            case SMITHING_TRIM -> {
                SmithingTrimDemoSlotDisplay smithingSlotDisplay = (SmithingTrimDemoSlotDisplay) display;

                MinecraftTypes.writeSlotDisplay(buf, smithingSlotDisplay.base());
                MinecraftTypes.writeSlotDisplay(buf, smithingSlotDisplay.material());
                MinecraftTypes.writeSlotDisplay(buf, smithingSlotDisplay.pattern());
            }
            case WITH_REMAINDER -> {
                WithRemainderSlotDisplay remainderSlotDisplay = (WithRemainderSlotDisplay) display;

                MinecraftTypes.writeSlotDisplay(buf, remainderSlotDisplay.input());
                MinecraftTypes.writeSlotDisplay(buf, remainderSlotDisplay.remainder());
            }
            case COMPOSITE -> MinecraftTypes.writeList(buf, ((CompositeSlotDisplay)display).contents(), MinecraftTypes::writeSlotDisplay);
        }
    }

    public static DataPalette readDataPalette(ByteBuf buf, PaletteType paletteType) {
        int bitsPerEntry = buf.readByte() & 0xFF;
        Palette palette = MinecraftTypes.readPalette(buf, paletteType, bitsPerEntry);
        BitStorage storage;
        if (!(palette instanceof SingletonPalette)) {
            storage = new BitStorage(bitsPerEntry, paletteType.getStorageSize(), MinecraftTypes.readLongArray(buf));
        } else {
            // Eat up - can be seen on Hypixel as of 1.19.0
            int length = MinecraftTypes.readVarInt(buf);
            for (int i = 0; i < length; i++) {
                buf.readLong();
            }
            storage = null;
        }

        return new DataPalette(palette, storage, paletteType);
    }

    /**
     * @deprecated globalPaletteBits is no longer in use, use {@link #readDataPalette(ByteBuf, PaletteType)} instead.
     */
    @Deprecated(forRemoval = true)
    public static DataPalette readDataPalette(ByteBuf buf, PaletteType paletteType, int globalPaletteBits) {
        return MinecraftTypes.readDataPalette(buf, paletteType);
    }

    public static void writeDataPalette(ByteBuf buf, DataPalette palette) {
        if (palette.getPalette() instanceof SingletonPalette) {
            buf.writeByte(0); // Bits per entry
            MinecraftTypes.writeVarInt(buf, palette.getPalette().idToState(0));
            MinecraftTypes.writeVarInt(buf, 0); // Data length
            return;
        }

        buf.writeByte(palette.getStorage().getBitsPerEntry());

        if (!(palette.getPalette() instanceof GlobalPalette)) {
            int paletteLength = palette.getPalette().size();
            MinecraftTypes.writeVarInt(buf, paletteLength);
            for (int i = 0; i < paletteLength; i++) {
                MinecraftTypes.writeVarInt(buf, palette.getPalette().idToState(i));
            }
        }

        long[] data = palette.getStorage().getData();
        MinecraftTypes.writeLongArray(buf, data);
    }

    private static Palette readPalette(ByteBuf buf, PaletteType paletteType, int bitsPerEntry) {
        if (bitsPerEntry == 0) {
            return new SingletonPalette(MinecraftTypes.readVarInt(buf));
        }
        if (bitsPerEntry <= paletteType.getMinBitsPerEntry()) {
            return new ListPalette(bitsPerEntry, buf);
        } else if (bitsPerEntry <= paletteType.getMaxBitsPerEntry()) {
            return new MapPalette(bitsPerEntry, buf);
        } else {
            return GlobalPalette.INSTANCE;
        }
    }

    public static ChunkSection readChunkSection(ByteBuf buf) {
        int blockCount = buf.readShort();

        DataPalette chunkPalette = MinecraftTypes.readDataPalette(buf, PaletteType.CHUNK);
        DataPalette biomePalette = MinecraftTypes.readDataPalette(buf, PaletteType.BIOME);
        return new ChunkSection(blockCount, chunkPalette, biomePalette);
    }

    /**
     * @deprecated globalBiomePaletteBits is no longer in use, use {@link #readChunkSection(ByteBuf)} instead.
     */
    @Deprecated(forRemoval = true)
    public static ChunkSection readChunkSection(ByteBuf buf, int globalBiomePaletteBits) {
        return MinecraftTypes.readChunkSection(buf);
    }

    public static void writeChunkSection(ByteBuf buf, ChunkSection section) {
        buf.writeShort(section.getBlockCount());
        MinecraftTypes.writeDataPalette(buf, section.getChunkData());
        MinecraftTypes.writeDataPalette(buf, section.getBiomeData());
    }

    public static <E extends Enum<E>> EnumSet<E> readEnumSet(ByteBuf buf, E[] values) {
        BitSet bitSet = MinecraftTypes.readFixedBitSet(buf, values.length);
        List<E> readValues = new ArrayList<>();

        for (int i = 0; i < values.length; i++) {
            if (bitSet.get(i)) {
                readValues.add(values[i]);
            }
        }

        return EnumSet.copyOf(readValues);
    }

    public static <E extends Enum<E>> void writeEnumSet(ByteBuf buf, EnumSet<E> enumSet, E[] values) {
        BitSet bitSet = new BitSet(values.length);

        for (int i = 0; i < values.length; i++) {
            bitSet.set(i, enumSet.contains(values[i]));
        }

        MinecraftTypes.writeFixedBitSet(buf, bitSet, values.length);
    }

    public static BitSet readFixedBitSet(ByteBuf buf, int length) {
        byte[] bytes = new byte[-Math.floorDiv(-length, 8)];
        buf.readBytes(bytes);
        return BitSet.valueOf(bytes);
    }

    public static void writeFixedBitSet(ByteBuf buf, BitSet bitSet, int length) {
        if (bitSet.length() > length) {
            throw new IllegalArgumentException("BitSet is larger than expected size (" + bitSet.length() + " > " + length + ")");
        } else {
            byte[] bytes = bitSet.toByteArray();
            buf.writeBytes(Arrays.copyOf(bytes, -Math.floorDiv(-length, 8)));
        }
    }

    public static GameProfile.Property readProperty(ByteBuf buf) {
        String name = MinecraftTypes.readString(buf);
        String value = MinecraftTypes.readString(buf);
        String signature = MinecraftTypes.readNullable(buf, MinecraftTypes::readString);
        return new GameProfile.Property(name, value, signature);
    }

    public static void writeProperty(ByteBuf buf, GameProfile.Property property) {
        MinecraftTypes.writeString(buf, property.getName());
        MinecraftTypes.writeString(buf, property.getValue());
        MinecraftTypes.writeNullable(buf, property.getSignature(), MinecraftTypes::writeString);
    }

    public static <T> T readById(ByteBuf buf, IntFunction<T> registry, Function<ByteBuf, T> custom) {
        int id = MinecraftTypes.readVarInt(buf);
        if (id == 0) {
            return custom.apply(buf);
        }
        return registry.apply(id - 1);
    }

    public static CustomSound readSoundEvent(ByteBuf buf) {
        String name = MinecraftTypes.readString(buf);
        boolean isNewSystem = buf.readBoolean();
        return new CustomSound(name, isNewSystem, isNewSystem ? buf.readFloat() : 16.0F);
    }

    public static void writeSoundEvent(ByteBuf buf, Sound soundEvent) {
        writeString(buf, soundEvent.getName());
        buf.writeBoolean(soundEvent.isNewSystem());
        if (soundEvent.isNewSystem()) {
            buf.writeFloat(soundEvent.getRange());
        }
    }
}
