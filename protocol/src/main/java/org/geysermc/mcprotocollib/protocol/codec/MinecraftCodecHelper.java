package org.geysermc.mcprotocollib.protocol.codec;

import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import lombok.RequiredArgsConstructor;
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
import org.geysermc.mcprotocollib.network.codec.BasePacketCodecHelper;
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
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemCodecHelper;
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
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplayType;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.FurnaceRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplay;
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

@RequiredArgsConstructor
public class MinecraftCodecHelper extends BasePacketCodecHelper {
    private static final int POSITION_X_SIZE = 38;
    private static final int POSITION_Y_SIZE = 12;
    private static final int POSITION_Z_SIZE = 38;
    private static final int POSITION_Y_SHIFT = 0xFFF;
    private static final int POSITION_WRITE_SHIFT = 0x3FFFFFF;

    @Nullable
    public <T> T readNullable(ByteBuf buf, Function<ByteBuf, T> ifPresent) {
        if (buf.readBoolean()) {
            return ifPresent.apply(buf);
        } else {
            return null;
        }
    }

    public <T> void writeNullable(ByteBuf buf, @Nullable T value, BiConsumer<ByteBuf, T> ifPresent) {
        if (value != null) {
            buf.writeBoolean(true);
            ifPresent.accept(buf, value);
        } else {
            buf.writeBoolean(false);
        }
    }

    public <T> List<T> readList(ByteBuf buf, Function<ByteBuf, T> reader) {
        int size = this.readVarInt(buf);
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(reader.apply(buf));
        }

        return list;
    }

    public <T> void writeList(ByteBuf buf, List<T> value, BiConsumer<ByteBuf, T> writer) {
        this.writeVarInt(buf, value.size());
        for (T t : value) {
            writer.accept(buf, t);
        }
    }

    public <T> Holder<T> readHolder(ByteBuf buf, Function<ByteBuf, T> readCustom) {
        int registryId = this.readVarInt(buf);
        return registryId == 0 ? Holder.ofCustom(readCustom.apply(buf)) : Holder.ofId(registryId - 1);
    }

    public <T> void writeHolder(ByteBuf buf, Holder<T> holder, BiConsumer<ByteBuf, T> writeCustom) {
        if (holder.isCustom()) {
            this.writeVarInt(buf, 0);
            writeCustom.accept(buf, holder.custom());
        } else {
            this.writeVarInt(buf, holder.id() + 1);
        }
    }

    public HolderSet readHolderSet(ByteBuf buf) {
        int length = this.readVarInt(buf) - 1;
        if (length == -1) {
            return new HolderSet(this.readResourceLocation(buf));
        } else {
            int[] holders = new int[length];
            for (int i = 0; i < length; i++) {
                holders[i] = this.readVarInt(buf);
            }

            return new HolderSet(holders);
        }
    }

    public void writeHolderSet(ByteBuf buf, HolderSet holderSet) {
        if (holderSet.getLocation() != null) {
            this.writeVarInt(buf, 0);
            this.writeResourceLocation(buf, holderSet.getLocation());
        } else {
            assert holderSet.getHolders() != null;
            this.writeVarInt(buf, holderSet.getHolders().length + 1);
            for (int holder : holderSet.getHolders()) {
                this.writeVarInt(buf, holder);
            }
        }
    }

    @SuppressWarnings("PatternValidation")
    public Key readResourceLocation(ByteBuf buf) {
        return Key.key(this.readString(buf));
    }

    public void writeResourceLocation(ByteBuf buf, Key location) {
        this.writeString(buf, location.asString());
    }

    public UUID readUUID(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    public void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public byte[] readByteArray(ByteBuf buf) {
        return this.readByteArray(buf, this::readVarInt);
    }

    public byte[] readByteArray(ByteBuf buf, ToIntFunction<ByteBuf> reader) {
        int length = reader.applyAsInt(buf);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }

    public void writeByteArray(ByteBuf buf, byte[] bytes) {
        this.writeByteArray(buf, bytes, this::writeVarInt);
    }

    public void writeByteArray(ByteBuf buf, byte[] bytes, ObjIntConsumer<ByteBuf> writer) {
        writer.accept(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public long[] readLongArray(ByteBuf buf) {
        return this.readLongArray(buf, this::readVarInt);
    }

    public long[] readLongArray(ByteBuf buf, ToIntFunction<ByteBuf> reader) {
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

    public void writeLongArray(ByteBuf buf, long[] l) {
        this.writeLongArray(buf, l, this::writeVarInt);
    }

    public void writeLongArray(ByteBuf buf, long[] l, ObjIntConsumer<ByteBuf> writer) {
        writer.accept(buf, l.length);
        for (long value : l) {
            buf.writeLong(value);
        }
    }

    @Nullable
    public NbtMap readCompoundTag(ByteBuf buf) {
        return readAnyTag(buf, NbtType.COMPOUND);
    }

    @NonNull
    public NbtMap readCompoundTagOrThrow(ByteBuf buf) {
        NbtMap tag = readCompoundTag(buf);
        if (tag == null) {
            throw new IllegalArgumentException("Got end-tag when trying to read NbtMap");
        }
        return tag;
    }

    @Nullable
    public <T> T readAnyTag(ByteBuf buf, NbtType<T> expected) {
        Object tag = this.readAnyTag(buf);

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
    public Object readAnyTag(ByteBuf buf) {
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

    public void writeAnyTag(ByteBuf buf, @Nullable Object tag) {
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
    public ItemStack readOptionalItemStack(ByteBuf buf) {
        int count = this.readVarInt(buf);
        if (count <= 0) {
            return null;
        }

        int item = this.readVarInt(buf);
        return new ItemStack(item, count, this.readDataComponentPatch(buf));
    }

    public void writeOptionalItemStack(ByteBuf buf, ItemStack item) {
        boolean empty = item == null || item.getAmount() <= 0;
        this.writeVarInt(buf, !empty ? item.getAmount() : 0);
        if (!empty) {
            this.writeVarInt(buf, item.getId());
            this.writeDataComponentPatch(buf, item.getDataComponents());
        }
    }

    @NotNull
    public ItemStack readItemStack(ByteBuf buf) {
        return this.readOptionalItemStack(buf);
    }

    public void writeItemStack(ByteBuf buf, @NotNull ItemStack item) {
        this.writeOptionalItemStack(buf, item);
    }

    @Nullable
    public DataComponents readDataComponentPatch(ByteBuf buf) {
        int nonNullComponents = this.readVarInt(buf);
        int nullComponents = this.readVarInt(buf);
        if (nonNullComponents == 0 && nullComponents == 0) {
            return null;
        }

        Map<DataComponentType<?>, DataComponent<?, ?>> dataComponents = new HashMap<>();
        for (int k = 0; k < nonNullComponents; k++) {
            DataComponentType<?> dataComponentType = DataComponentType.from(this.readVarInt(buf));
            DataComponent<?, ?> dataComponent = dataComponentType.readDataComponent(ItemCodecHelper.INSTANCE, buf);
            dataComponents.put(dataComponentType, dataComponent);
        }

        for (int k = 0; k < nullComponents; k++) {
            DataComponentType<?> dataComponentType = DataComponentType.from(this.readVarInt(buf));
            DataComponent<?, ?> dataComponent = dataComponentType.readNullDataComponent();
            dataComponents.put(dataComponentType, dataComponent);
        }

        return new DataComponents(dataComponents);
    }

    public void writeDataComponentPatch(ByteBuf buf, DataComponents dataComponents) {
        if (dataComponents == null) {
            this.writeVarInt(buf, 0);
            this.writeVarInt(buf, 0);
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

            this.writeVarInt(buf, i);
            this.writeVarInt(buf, j);

            for (DataComponent<?, ?> component : dataComponents.getDataComponents().values()) {
                if (component.getValue() != null) {
                    this.writeVarInt(buf, component.getType().getId());
                    component.write(ItemCodecHelper.INSTANCE, buf);
                }
            }

            for (DataComponent<?, ?> component : dataComponents.getDataComponents().values()) {
                if (component.getValue() == null) {
                    this.writeVarInt(buf, component.getType().getId());
                }
            }
        }
    }

    @NotNull
    public ItemStack readTradeItemStack(ByteBuf buf) {
        int item = this.readVarInt(buf);
        int count = this.readVarInt(buf);
        int componentsLength = this.readVarInt(buf);

        Map<DataComponentType<?>, DataComponent<?, ?>> dataComponents = new HashMap<>();
        for (int i = 0; i < componentsLength; i++) {
            DataComponentType<?> dataComponentType = DataComponentType.from(this.readVarInt(buf));
            DataComponent<?, ?> dataComponent = dataComponentType.readDataComponent(ItemCodecHelper.INSTANCE, buf);
            dataComponents.put(dataComponentType, dataComponent);
        }

        return new ItemStack(item, count, new DataComponents(dataComponents));
    }

    public void writeTradeItemStack(ByteBuf buf, @NotNull ItemStack item) {
        this.writeVarInt(buf, item.getId());
        this.writeVarInt(buf, item.getAmount());

        DataComponents dataComponents = item.getDataComponents();
        if (dataComponents == null) {
            this.writeVarInt(buf, 0);
            return;
        }

        this.writeVarInt(buf, dataComponents.getDataComponents().size());
        for (DataComponent<?, ?> component : dataComponents.getDataComponents().values()) {
            this.writeVarInt(buf, component.getType().getId());
            component.write(ItemCodecHelper.INSTANCE, buf);
        }
    }

    public Vector3i readPosition(ByteBuf buf) {
        long val = buf.readLong();

        int x = (int) (val >> POSITION_X_SIZE);
        int y = (int) (val << 52 >> 52);
        int z = (int) (val << 26 >> POSITION_Z_SIZE);

        return Vector3i.from(x, y, z);
    }

    public void writePosition(ByteBuf buf, Vector3i pos) {
        long x = pos.getX() & POSITION_WRITE_SHIFT;
        long y = pos.getY() & POSITION_Y_SHIFT;
        long z = pos.getZ() & POSITION_WRITE_SHIFT;

        buf.writeLong(x << POSITION_X_SIZE | z << POSITION_Y_SIZE | y);
    }

    public Vector3f readRotation(ByteBuf buf) {
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();

        return Vector3f.from(x, y, z);
    }

    public void writeRotation(ByteBuf buf, Vector3f rot) {
        buf.writeFloat(rot.getX());
        buf.writeFloat(rot.getY());
        buf.writeFloat(rot.getZ());
    }

    public Vector4f readQuaternion(ByteBuf buf) {
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        float w = buf.readFloat();

        return Vector4f.from(x, y, z, w);
    }

    public void writeQuaternion(ByteBuf buf, Vector4f vec4) {
        buf.writeFloat(vec4.getX());
        buf.writeFloat(vec4.getY());
        buf.writeFloat(vec4.getZ());
        buf.writeFloat(vec4.getW());
    }

    public Direction readDirection(ByteBuf buf) {
        return Direction.from(this.readVarInt(buf));
    }

    public void writeDirection(ByteBuf buf, Direction dir) {
        this.writeEnum(buf, dir);
    }

    public Pose readPose(ByteBuf buf) {
        return Pose.from(this.readVarInt(buf));
    }

    public void writePose(ByteBuf buf, Pose pose) {
        this.writeEnum(buf, pose);
    }

    public Holder<WolfVariant> readWolfVariant(ByteBuf buf) {
        return this.readHolder(buf, input -> {
            Key wildTexture = this.readResourceLocation(input);
            Key tameTexture = this.readResourceLocation(input);
            Key angryTexture = this.readResourceLocation(input);
            Key biomeLocation = null;
            int[] biomeHolders = null;

            int length = this.readVarInt(input) - 1;
            if (length == -1) {
                biomeLocation = this.readResourceLocation(input);
            } else {
                biomeHolders = new int[length];
                for (int j = 0; j < length; j++) {
                    biomeHolders[j] = this.readVarInt(input);
                }
            }
            return new WolfVariant(wildTexture, tameTexture, angryTexture, biomeLocation, biomeHolders);
        });
    }

    public void writeWolfVariant(ByteBuf buf, Holder<WolfVariant> variantHolder) {
        this.writeHolder(buf, variantHolder, (output, variant) -> {
            this.writeResourceLocation(output, variant.wildTexture());
            this.writeResourceLocation(output, variant.tameTexture());
            this.writeResourceLocation(output, variant.angryTexture());
            if (variant.biomeLocation() != null) {
                this.writeVarInt(output, 0);
                this.writeResourceLocation(output, variant.biomeLocation());
            } else {
                this.writeVarInt(output, variant.biomeHolders().length + 1);
                for (int holder : variant.biomeHolders()) {
                    this.writeVarInt(output, holder);
                }
            }
        });
    }

    public Holder<PaintingVariant> readPaintingVariant(ByteBuf buf) {
        return this.readHolder(buf, input -> {
            return new PaintingVariant(this.readVarInt(input), this.readVarInt(input), this.readResourceLocation(input),
                    this.readNullable(input, this::readComponent), this.readNullable(input, this::readComponent));
        });
    }

    public void writePaintingVariant(ByteBuf buf, Holder<PaintingVariant> variantHolder) {
        this.writeHolder(buf, variantHolder, (output, variant) -> {
            this.writeVarInt(buf, variant.width());
            this.writeVarInt(buf, variant.height());
            this.writeResourceLocation(buf, variant.assetId());
            this.writeNullable(buf, variant.title(), this::writeComponent);
            this.writeNullable(buf, variant.author(), this::writeComponent);
        });
    }

    public SnifferState readSnifferState(ByteBuf buf) {
        return SnifferState.from(this.readVarInt(buf));
    }

    public void writeSnifferState(ByteBuf buf, SnifferState state) {
        this.writeEnum(buf, state);
    }

    public ArmadilloState readArmadilloState(ByteBuf buf) {
        return ArmadilloState.from(this.readVarInt(buf));
    }

    public void writeArmadilloState(ByteBuf buf, ArmadilloState state) {
        this.writeEnum(buf, state);
    }

    private void writeEnum(ByteBuf buf, Enum<?> e) {
        this.writeVarInt(buf, e.ordinal());
    }

    public Component readComponent(ByteBuf buf) {
        // do not use NbtMap, as mojang serializes a plaintext component as just a single StringTag
        Object tag = readAnyTag(buf);
        if (tag == null) {
            throw new IllegalArgumentException("Got end-tag when trying to read Component");
        }
        JsonElement json = NbtComponentSerializer.tagComponentToJson(tag);
        return DefaultComponentSerializer.get().deserializeFromTree(json);
    }

    public void writeComponent(ByteBuf buf, Component component) {
        JsonElement json = DefaultComponentSerializer.get().serializeToTree(component);
        Object tag = NbtComponentSerializer.jsonComponentToTag(json);
        writeAnyTag(buf, tag);
    }

    public EntityMetadata<?, ?>[] readEntityMetadata(ByteBuf buf) {
        List<EntityMetadata<?, ?>> ret = new ArrayList<>();
        int id;
        while ((id = buf.readUnsignedByte()) != 255) {
            ret.add(this.readMetadata(buf, id));
        }

        return ret.toArray(new EntityMetadata<?, ?>[0]);
    }

    public void writeEntityMetadata(ByteBuf buf, EntityMetadata<?, ?>[] metadata) {
        for (EntityMetadata<?, ?> meta : metadata) {
            this.writeMetadata(buf, meta);
        }

        buf.writeByte(255);
    }

    public EntityMetadata<?, ?> readMetadata(ByteBuf buf, int id) {
        MetadataType<?> type = this.readMetadataType(buf);
        return type.readMetadata(this, buf, id);
    }

    public void writeMetadata(ByteBuf buf, EntityMetadata<?, ?> metadata) {
        buf.writeByte(metadata.getId());
        this.writeMetadataType(buf, metadata.getType());
        metadata.write(this, buf);
    }

    public MetadataType<?> readMetadataType(ByteBuf buf) {
        int id = this.readVarInt(buf);
        if (id >= MetadataType.size()) {
            throw new IllegalArgumentException("Received id " + id + " for MetadataType when the maximum was " + MetadataType.size() + "!");
        }

        return MetadataType.from(id);
    }

    public void writeMetadataType(ByteBuf buf, MetadataType<?> type) {
        this.writeVarInt(buf, type.getId());
    }

    public GlobalPos readGlobalPos(ByteBuf buf) {
        Key dimension = readResourceLocation(buf);
        Vector3i pos = this.readPosition(buf);
        return new GlobalPos(dimension, pos);
    }

    public void writeGlobalPos(ByteBuf buf, GlobalPos pos) {
        this.writeResourceLocation(buf, pos.getDimension());
        this.writePosition(buf, pos.getPosition());
    }

    public PlayerSpawnInfo readPlayerSpawnInfo(ByteBuf buf) {
        int dimension = this.readVarInt(buf);
        Key worldName = this.readResourceLocation(buf);
        long hashedSeed = buf.readLong();
        GameMode gameMode = GameMode.byId(buf.readByte());
        GameMode previousGamemode = GameMode.byNullableId(buf.readByte());
        boolean debug = buf.readBoolean();
        boolean flat = buf.readBoolean();
        GlobalPos lastDeathPos = this.readNullable(buf, this::readGlobalPos);
        int portalCooldown = this.readVarInt(buf);
        int seaLevel = this.readVarInt(buf);
        return new PlayerSpawnInfo(dimension, worldName, hashedSeed, gameMode, previousGamemode, debug, flat, lastDeathPos, portalCooldown, seaLevel);
    }

    public void writePlayerSpawnInfo(ByteBuf buf, PlayerSpawnInfo info) {
        this.writeVarInt(buf, info.getDimension());
        this.writeResourceLocation(buf, info.getWorldName());
        buf.writeLong(info.getHashedSeed());
        buf.writeByte(info.getGameMode().ordinal());
        buf.writeByte(GameMode.toNullableId(info.getPreviousGamemode()));
        buf.writeBoolean(info.isDebug());
        buf.writeBoolean(info.isFlat());
        this.writeNullable(buf, info.getLastDeathPos(), this::writeGlobalPos);
        this.writeVarInt(buf, info.getPortalCooldown());
        this.writeVarInt(buf, info.getSeaLevel());
    }

    public ParticleType readParticleType(ByteBuf buf) {
        return ParticleType.from(this.readVarInt(buf));
    }

    public void writeParticleType(ByteBuf buf, ParticleType type) {
        this.writeEnum(buf, type);
    }

    public Particle readParticle(ByteBuf buf) {
        ParticleType particleType = this.readParticleType(buf);
        return new Particle(particleType, this.readParticleData(buf, particleType));
    }

    public void writeParticle(ByteBuf buf, Particle particle) {
        this.writeEnum(buf, particle.getType());
        this.writeParticleData(buf, particle.getType(), particle.getData());
    }

    public ParticleData readParticleData(ByteBuf buf, ParticleType type) {
        return switch (type) {
            case BLOCK, BLOCK_MARKER, FALLING_DUST, DUST_PILLAR, BLOCK_CRUMBLE -> new BlockParticleData(this.readVarInt(buf));
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
            case ITEM -> new ItemParticleData(this.readOptionalItemStack(buf));
            case SCULK_CHARGE -> new SculkChargeParticleData(buf.readFloat());
            case SHRIEK -> new ShriekParticleData(this.readVarInt(buf));
            case TRAIL -> new TrailParticleData(Vector3d.from(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readInt(), this.readVarInt(buf));
            case VIBRATION -> new VibrationParticleData(this.readPositionSource(buf), this.readVarInt(buf));
            default -> null;
        };
    }

    public void writeParticleData(ByteBuf buf, ParticleType type, ParticleData data) {
        switch (type) {
            case BLOCK, BLOCK_MARKER, FALLING_DUST, DUST_PILLAR, BLOCK_CRUMBLE -> {
                BlockParticleData blockData = (BlockParticleData) data;
                this.writeVarInt(buf, blockData.getBlockState());
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
                this.writeOptionalItemStack(buf, itemData.getItemStack());
            }
            case SCULK_CHARGE -> {
                SculkChargeParticleData sculkData = (SculkChargeParticleData) data;
                buf.writeFloat(sculkData.getRoll());
            }
            case SHRIEK -> {
                ShriekParticleData shriekData = (ShriekParticleData) data;
                this.writeVarInt(buf, shriekData.getDelay());
            }
            case TRAIL -> {
                TrailParticleData trailData = (TrailParticleData) data;
                buf.writeDouble(trailData.target().getX());
                buf.writeDouble(trailData.target().getY());
                buf.writeDouble(trailData.target().getZ());
                buf.writeInt(trailData.color());
                this.writeVarInt(buf, trailData.duration());
            }
            case VIBRATION -> {
                VibrationParticleData vibrationData = (VibrationParticleData) data;
                this.writePositionSource(buf, vibrationData.getPositionSource());
                this.writeVarInt(buf, vibrationData.getArrivalTicks());
            }
        }
    }

    public NumberFormat readNumberFormat(ByteBuf buf) {
        int id = this.readVarInt(buf);
        return switch (id) {
            case 0 -> BlankFormat.INSTANCE;
            case 1 -> new StyledFormat(this.readCompoundTagOrThrow(buf));
            case 2 -> new FixedFormat(this.readComponent(buf));
            default -> throw new IllegalArgumentException("Unknown number format type: " + id);
        };
    }

    public void writeNumberFormat(ByteBuf buf, NumberFormat numberFormat) {
        if (numberFormat instanceof BlankFormat) {
            this.writeVarInt(buf, 0);
        } else if (numberFormat instanceof StyledFormat styledFormat) {
            this.writeVarInt(buf, 1);
            this.writeAnyTag(buf, styledFormat.getStyle());
        } else if (numberFormat instanceof FixedFormat fixedFormat) {
            this.writeVarInt(buf, 2);
            this.writeComponent(buf, fixedFormat.getValue());
        } else {
            throw new IllegalArgumentException("Unknown number format: " + numberFormat);
        }
    }

    public ChatType readChatType(ByteBuf buf) {
        return new ChatType(readChatTypeDecoration(buf), readChatTypeDecoration(buf));
    }

    public void writeChatType(ByteBuf buf, ChatType chatType) {
        this.writeChatTypeDecoration(buf, chatType.chat());
        this.writeChatTypeDecoration(buf, chatType.narration());
    }

    public ChatTypeDecoration readChatTypeDecoration(ByteBuf buf) {
        String translationKey = this.readString(buf);

        int size = this.readVarInt(buf);
        List<ChatTypeDecoration.Parameter> parameters = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            parameters.add(ChatTypeDecoration.Parameter.VALUES[this.readVarInt(buf)]);
        }

        NbtMap style = this.readCompoundTag(buf);
        return new ChatType.ChatTypeDecorationImpl(translationKey, parameters, style);
    }

    public void writeChatTypeDecoration(ByteBuf buf, ChatTypeDecoration decoration) {
        this.writeString(buf, decoration.translationKey());

        this.writeVarInt(buf, decoration.parameters().size());
        for (ChatTypeDecoration.Parameter parameter : decoration.parameters()) {
            this.writeVarInt(buf, parameter.ordinal());
        }

        this.writeAnyTag(buf, decoration.style());
    }

    public PositionSource readPositionSource(ByteBuf buf) {
        PositionSourceType type = PositionSourceType.from(this.readVarInt(buf));
        return switch (type) {
            case BLOCK -> new BlockPositionSource(this.readPosition(buf));
            case ENTITY -> new EntityPositionSource(this.readVarInt(buf), buf.readFloat());
        };
    }

    public void writePositionSource(ByteBuf buf, PositionSource positionSource) {
        this.writeVarInt(buf, positionSource.getType().ordinal());
        if (positionSource instanceof BlockPositionSource blockPositionSource) {
            this.writePosition(buf, blockPositionSource.getPosition());
        } else if (positionSource instanceof EntityPositionSource entityPositionSource) {
            this.writeVarInt(buf, entityPositionSource.getEntityId());
            buf.writeFloat(entityPositionSource.getYOffset());
        } else {
            throw new IllegalStateException("Unknown position source type!");
        }
    }

    public VillagerData readVillagerData(ByteBuf buf) {
        return new VillagerData(this.readVarInt(buf), this.readVarInt(buf), this.readVarInt(buf));
    }

    public void writeVillagerData(ByteBuf buf, VillagerData villagerData) {
        this.writeVarInt(buf, villagerData.getType());
        this.writeVarInt(buf, villagerData.getProfession());
        this.writeVarInt(buf, villagerData.getLevel());
    }

    public ModifierOperation readModifierOperation(ByteBuf buf) {
        return ModifierOperation.from(buf.readByte());
    }

    public void writeModifierOperation(ByteBuf buf, ModifierOperation operation) {
        buf.writeByte(operation.ordinal());
    }

    public Effect readEffect(ByteBuf buf) {
        return Effect.from(this.readVarInt(buf));
    }

    public void writeEffect(ByteBuf buf, Effect effect) {
        this.writeVarInt(buf, effect.ordinal());
    }

    public BlockBreakStage readBlockBreakStage(ByteBuf buf) {
        int stage = buf.readUnsignedByte();
        if (stage >= 0 && stage < 10) {
            return BlockBreakStage.STAGES[stage];
        } else {
            return BlockBreakStage.RESET;
        }
    }

    public void writeBlockBreakStage(ByteBuf buf, BlockBreakStage stage) {
        if (stage == BlockBreakStage.RESET) {
            buf.writeByte(255);
        } else {
            buf.writeByte(stage.ordinal());
        }
    }

    @Nullable
    public BlockEntityType readBlockEntityType(ByteBuf buf) {
        return BlockEntityType.from(this.readVarInt(buf));
    }

    public void writeBlockEntityType(ByteBuf buf, BlockEntityType type) {
        this.writeEnum(buf, type);
    }

    public LightUpdateData readLightUpdateData(ByteBuf buf) {
        BitSet skyYMask = BitSet.valueOf(this.readLongArray(buf));
        BitSet blockYMask = BitSet.valueOf(this.readLongArray(buf));
        BitSet emptySkyYMask = BitSet.valueOf(this.readLongArray(buf));
        BitSet emptyBlockYMask = BitSet.valueOf(this.readLongArray(buf));

        int skyUpdateSize = this.readVarInt(buf);
        List<byte[]> skyUpdates = new ArrayList<>(skyUpdateSize);
        for (int i = 0; i < skyUpdateSize; i++) {
            skyUpdates.add(this.readByteArray(buf));
        }

        int blockUpdateSize = this.readVarInt(buf);
        List<byte[]> blockUpdates = new ArrayList<>(blockUpdateSize);
        for (int i = 0; i < blockUpdateSize; i++) {
            blockUpdates.add(this.readByteArray(buf));
        }

        return new LightUpdateData(skyYMask, blockYMask, emptySkyYMask, emptyBlockYMask, skyUpdates, blockUpdates);
    }

    public void writeLightUpdateData(ByteBuf buf, LightUpdateData data) {
        writeBitSet(buf, data.getSkyYMask());
        writeBitSet(buf, data.getBlockYMask());
        writeBitSet(buf, data.getEmptySkyYMask());
        writeBitSet(buf, data.getEmptyBlockYMask());

        this.writeVarInt(buf, data.getSkyUpdates().size());
        for (byte[] array : data.getSkyUpdates()) {
            this.writeByteArray(buf, array);
        }

        this.writeVarInt(buf, data.getBlockUpdates().size());
        for (byte[] array : data.getBlockUpdates()) {
            this.writeByteArray(buf, array);
        }
    }

    private void writeBitSet(ByteBuf buf, BitSet bitSet) {
        long[] array = bitSet.toLongArray();
        this.writeLongArray(buf, array);
    }

    public LevelEvent readLevelEvent(ByteBuf buf) {
        int id = buf.readInt();
        LevelEventType type = LevelEventType.from(id);
        if (type != null) {
            return type;
        }
        return new UnknownLevelEvent(id);
    }

    public void writeLevelEvent(ByteBuf buf, LevelEvent event) {
        buf.writeInt(event.getId());
    }

    public StatisticCategory readStatisticCategory(ByteBuf buf) {
        return StatisticCategory.from(this.readVarInt(buf));
    }

    public void writeStatisticCategory(ByteBuf buf, StatisticCategory category) {
        this.writeEnum(buf, category);
    }

    public SoundCategory readSoundCategory(ByteBuf buf) {
        return SoundCategory.from(this.readVarInt(buf));
    }

    public void writeSoundCategory(ByteBuf buf, SoundCategory category) {
        this.writeEnum(buf, category);
    }

    public EntityEvent readEntityEvent(ByteBuf buf) {
        return EntityEvent.from(buf.readByte());
    }

    public void writeEntityEvent(ByteBuf buf, EntityEvent event) {
        buf.writeByte(event.ordinal());
    }

    public Ingredient readRecipeIngredient(ByteBuf buf) {
        return new Ingredient(this.readHolderSet(buf));
    }

    public void writeRecipeIngredient(ByteBuf buf, Ingredient ingredient) {
        this.writeHolderSet(buf, ingredient.getValues());
    }

    public RecipeDisplay readRecipeDisplay(ByteBuf buf) {
        RecipeDisplayType type = RecipeDisplayType.from(this.readVarInt(buf));
        RecipeDisplay display;
        switch (type) {
            case CRAFTING_SHAPELESS -> {
                List<SlotDisplay> ingredients = this.readList(buf, this::readSlotDisplay);
                SlotDisplay result = this.readSlotDisplay(buf);
                SlotDisplay craftingStation = this.readSlotDisplay(buf);

                display = new ShapelessCraftingRecipeDisplay(ingredients, result, craftingStation);
            }
            case CRAFTING_SHAPED -> {
                int width = this.readVarInt(buf);
                int height = this.readVarInt(buf);
                List<SlotDisplay> ingredients = this.readList(buf, this::readSlotDisplay);
                SlotDisplay result = this.readSlotDisplay(buf);
                SlotDisplay craftingStation = this.readSlotDisplay(buf);

                display = new ShapedCraftingRecipeDisplay(width, height, ingredients, result, craftingStation);
            }
            case FURNACE -> {
                SlotDisplay ingredient = this.readSlotDisplay(buf);
                SlotDisplay fuel = this.readSlotDisplay(buf);
                SlotDisplay result = this.readSlotDisplay(buf);
                SlotDisplay craftingStation = this.readSlotDisplay(buf);
                int duration = this.readVarInt(buf);
                float experience = buf.readFloat();

                display = new FurnaceRecipeDisplay(ingredient, fuel, result, craftingStation, duration, experience);
            }
            case STONECUTTER -> {
                SlotDisplay input = this.readSlotDisplay(buf);
                SlotDisplay result = this.readSlotDisplay(buf);
                SlotDisplay craftingStation = this.readSlotDisplay(buf);

                display = new StonecutterRecipeDisplay(input, result, craftingStation);
            }
            case SMITHING -> {
                SlotDisplay template = this.readSlotDisplay(buf);
                SlotDisplay base = this.readSlotDisplay(buf);
                SlotDisplay addition = this.readSlotDisplay(buf);
                SlotDisplay result = this.readSlotDisplay(buf);
                SlotDisplay craftingStation = this.readSlotDisplay(buf);

                display = new SmithingRecipeDisplay(template, base, addition, result, craftingStation);
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
        return display;
    }

    public void writeRecipeDisplay(ByteBuf buf, RecipeDisplay display) {
        this.writeVarInt(buf, display.getType().ordinal());
        switch (display.getType()) {
            case CRAFTING_SHAPELESS -> {
                ShapelessCraftingRecipeDisplay shapelessDisplay = (ShapelessCraftingRecipeDisplay) display;

                this.writeList(buf, shapelessDisplay.ingredients(), this::writeSlotDisplay);
                this.writeSlotDisplay(buf, shapelessDisplay.result());
                this.writeSlotDisplay(buf, shapelessDisplay.craftingStation());
            }
            case CRAFTING_SHAPED -> {
                ShapedCraftingRecipeDisplay shapedDisplay = (ShapedCraftingRecipeDisplay) display;

                this.writeVarInt(buf, shapedDisplay.width());
                this.writeVarInt(buf, shapedDisplay.height());
                this.writeList(buf, shapedDisplay.ingredients(), this::writeSlotDisplay);
                this.writeSlotDisplay(buf, shapedDisplay.result());
                this.writeSlotDisplay(buf, shapedDisplay.craftingStation());
            }
            case FURNACE -> {
                FurnaceRecipeDisplay furnaceDisplay = (FurnaceRecipeDisplay) display;

                this.writeSlotDisplay(buf, furnaceDisplay.ingredient());
                this.writeSlotDisplay(buf, furnaceDisplay.fuel());
                this.writeSlotDisplay(buf, furnaceDisplay.result());
                this.writeSlotDisplay(buf, furnaceDisplay.craftingStation());
                this.writeVarInt(buf, furnaceDisplay.duration());
                buf.writeFloat(furnaceDisplay.experience());
            }
            case STONECUTTER -> {
                StonecutterRecipeDisplay stonecutterDisplay = (StonecutterRecipeDisplay) display;

                this.writeSlotDisplay(buf, stonecutterDisplay.input());
                this.writeSlotDisplay(buf, stonecutterDisplay.result());
                this.writeSlotDisplay(buf, stonecutterDisplay.craftingStation());
            }
            case SMITHING -> {
                SmithingRecipeDisplay smithingDisplay = (SmithingRecipeDisplay) display;

                this.writeSlotDisplay(buf, smithingDisplay.template());
                this.writeSlotDisplay(buf, smithingDisplay.base());
                this.writeSlotDisplay(buf, smithingDisplay.addition());
                this.writeSlotDisplay(buf, smithingDisplay.result());
                this.writeSlotDisplay(buf, smithingDisplay.craftingStation());
            }
        }
    }

    public SlotDisplay readSlotDisplay(ByteBuf buf) {
        RecipeSlotType type = RecipeSlotType.from(this.readVarInt(buf));
        SlotDisplay display;
        switch (type) {
            case EMPTY -> display = EmptySlotDisplay.INSTANCE;
            case ANY_FUEL -> display = new AnyFuelSlotDisplay();
            case ITEM -> display = new ItemSlotDisplay(this.readVarInt(buf));
            case ITEM_STACK -> display = new ItemStackSlotDisplay(this.readItemStack(buf));
            case TAG -> display = new TagSlotDisplay(this.readResourceLocation(buf));
            case SMITHING_TRIM -> display = new SmithingTrimDemoSlotDisplay(this.readSlotDisplay(buf), this.readSlotDisplay(buf), this.readSlotDisplay(buf));
            case WITH_REMAINDER -> display = new WithRemainderSlotDisplay(this.readSlotDisplay(buf), this.readSlotDisplay(buf));
            case COMPOSITE -> display = new CompositeSlotDisplay(this.readList(buf, this::readSlotDisplay));
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
        return display;
    }

    public void writeSlotDisplay(ByteBuf buf, SlotDisplay display) {
        this.writeVarInt(buf, display.getType().ordinal());
        switch (display.getType()) {
            case ITEM -> this.writeVarInt(buf, ((ItemSlotDisplay)display).item());
            case ITEM_STACK -> this.writeItemStack(buf, ((ItemStackSlotDisplay)display).itemStack());
            case TAG -> this.writeResourceLocation(buf, ((TagSlotDisplay)display).tag());
            case SMITHING_TRIM -> {
                SmithingTrimDemoSlotDisplay smithingSlotDisplay = (SmithingTrimDemoSlotDisplay) display;

                this.writeSlotDisplay(buf, smithingSlotDisplay.base());
                this.writeSlotDisplay(buf, smithingSlotDisplay.material());
                this.writeSlotDisplay(buf, smithingSlotDisplay.pattern());
            }
            case WITH_REMAINDER -> {
                WithRemainderSlotDisplay remainderSlotDisplay = (WithRemainderSlotDisplay) display;

                this.writeSlotDisplay(buf, remainderSlotDisplay.input());
                this.writeSlotDisplay(buf, remainderSlotDisplay.remainder());
            }
            case COMPOSITE -> this.writeList(buf, ((CompositeSlotDisplay)display).contents(), this::writeSlotDisplay);
        }
    }

    public DataPalette readDataPalette(ByteBuf buf, PaletteType paletteType) {
        int bitsPerEntry = buf.readByte() & 0xFF;
        Palette palette = this.readPalette(buf, paletteType, bitsPerEntry);
        BitStorage storage;
        if (!(palette instanceof SingletonPalette)) {
            storage = new BitStorage(bitsPerEntry, paletteType.getStorageSize(), this.readLongArray(buf));
        } else {
            // Eat up - can be seen on Hypixel as of 1.19.0
            int length = this.readVarInt(buf);
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
    public DataPalette readDataPalette(ByteBuf buf, PaletteType paletteType, int globalPaletteBits) {
        return this.readDataPalette(buf, paletteType);
    }

    public void writeDataPalette(ByteBuf buf, DataPalette palette) {
        if (palette.getPalette() instanceof SingletonPalette) {
            buf.writeByte(0); // Bits per entry
            this.writeVarInt(buf, palette.getPalette().idToState(0));
            this.writeVarInt(buf, 0); // Data length
            return;
        }

        buf.writeByte(palette.getStorage().getBitsPerEntry());

        if (!(palette.getPalette() instanceof GlobalPalette)) {
            int paletteLength = palette.getPalette().size();
            this.writeVarInt(buf, paletteLength);
            for (int i = 0; i < paletteLength; i++) {
                this.writeVarInt(buf, palette.getPalette().idToState(i));
            }
        }

        long[] data = palette.getStorage().getData();
        this.writeLongArray(buf, data);
    }

    private Palette readPalette(ByteBuf buf, PaletteType paletteType, int bitsPerEntry) {
        if (bitsPerEntry == 0) {
            return new SingletonPalette(this.readVarInt(buf));
        }
        if (bitsPerEntry <= paletteType.getMinBitsPerEntry()) {
            return new ListPalette(bitsPerEntry, buf, this);
        } else if (bitsPerEntry <= paletteType.getMaxBitsPerEntry()) {
            return new MapPalette(bitsPerEntry, buf, this);
        } else {
            return GlobalPalette.INSTANCE;
        }
    }

    public ChunkSection readChunkSection(ByteBuf buf) {
        int blockCount = buf.readShort();

        DataPalette chunkPalette = this.readDataPalette(buf, PaletteType.CHUNK);
        DataPalette biomePalette = this.readDataPalette(buf, PaletteType.BIOME);
        return new ChunkSection(blockCount, chunkPalette, biomePalette);
    }

    /**
     * @deprecated globalBiomePaletteBits is no longer in use, use {@link #readChunkSection(ByteBuf)} instead.
     */
    @Deprecated(forRemoval = true)
    public ChunkSection readChunkSection(ByteBuf buf, int globalBiomePaletteBits) {
        return this.readChunkSection(buf);
    }

    public void writeChunkSection(ByteBuf buf, ChunkSection section) {
        buf.writeShort(section.getBlockCount());
        this.writeDataPalette(buf, section.getChunkData());
        this.writeDataPalette(buf, section.getBiomeData());
    }

    public <E extends Enum<E>> EnumSet<E> readEnumSet(ByteBuf buf, E[] values) {
        BitSet bitSet = this.readFixedBitSet(buf, values.length);
        List<E> readValues = new ArrayList<>();

        for (int i = 0; i < values.length; i++) {
            if (bitSet.get(i)) {
                readValues.add(values[i]);
            }
        }

        return EnumSet.copyOf(readValues);
    }

    public <E extends Enum<E>> void writeEnumSet(ByteBuf buf, EnumSet<E> enumSet, E[] values) {
        BitSet bitSet = new BitSet(values.length);

        for (int i = 0; i < values.length; i++) {
            bitSet.set(i, enumSet.contains(values[i]));
        }

        this.writeFixedBitSet(buf, bitSet, values.length);
    }

    public BitSet readFixedBitSet(ByteBuf buf, int length) {
        byte[] bytes = new byte[-Math.floorDiv(-length, 8)];
        buf.readBytes(bytes);
        return BitSet.valueOf(bytes);
    }

    public void writeFixedBitSet(ByteBuf buf, BitSet bitSet, int length) {
        if (bitSet.length() > length) {
            throw new IllegalArgumentException("BitSet is larger than expected size (" + bitSet.length() + " > " + length + ")");
        } else {
            byte[] bytes = bitSet.toByteArray();
            buf.writeBytes(Arrays.copyOf(bytes, -Math.floorDiv(-length, 8)));
        }
    }

    public GameProfile.Property readProperty(ByteBuf buf) {
        String name = this.readString(buf);
        String value = this.readString(buf);
        String signature = this.readNullable(buf, this::readString);
        return new GameProfile.Property(name, value, signature);
    }

    public void writeProperty(ByteBuf buf, GameProfile.Property property) {
        this.writeString(buf, property.getName());
        this.writeString(buf, property.getValue());
        this.writeNullable(buf, property.getSignature(), this::writeString);
    }

    public <T> T readById(ByteBuf buf, IntFunction<T> registry, Function<ByteBuf, T> custom) {
        int id = this.readVarInt(buf);
        if (id == 0) {
            return custom.apply(buf);
        }
        return registry.apply(id - 1);
    }

    public CustomSound readSoundEvent(ByteBuf buf) {
        String name = this.readString(buf);
        boolean isNewSystem = buf.readBoolean();
        return new CustomSound(name, isNewSystem, isNewSystem ? buf.readFloat() : 16.0F);
    }

    public void writeSoundEvent(ByteBuf buf, Sound soundEvent) {
        writeString(buf, soundEvent.getName());
        buf.writeBoolean(soundEvent.isNewSystem());
        if (soundEvent.isNewSystem()) {
            buf.writeFloat(soundEvent.getRange());
        }
    }
}
