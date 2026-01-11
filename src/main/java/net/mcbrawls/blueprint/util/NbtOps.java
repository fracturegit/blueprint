package net.mcbrawls.blueprint.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.kyori.adventure.nbt.ArrayBinaryTag;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.NumberBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class NbtOps implements DynamicOps<BinaryTag> {
    public static final NbtOps INSTANCE = new NbtOps();

    private NbtOps() {
    }

    public BinaryTag empty() {
        return EndBinaryTag.endBinaryTag();
    }

    public <U> U convertTo(DynamicOps<U> dynamicOps, BinaryTag binaryTag) {
        return switch (binaryTag) {
            case EndBinaryTag nbtEnd -> dynamicOps.empty();
            case ByteBinaryTag tag -> dynamicOps.createByte(tag.value());
            case ShortBinaryTag tag -> dynamicOps.createShort(tag.value());
            case IntBinaryTag tag -> dynamicOps.createInt(tag.value());
            case LongBinaryTag tag -> dynamicOps.createLong(tag.value());
            case FloatBinaryTag tag -> dynamicOps.createFloat(tag.value());
            case DoubleBinaryTag tag -> dynamicOps.createDouble(tag.value());
            case ByteArrayBinaryTag nbtByteArray -> dynamicOps.createByteList(ByteBuffer.wrap(nbtByteArray.value()));
            case StringBinaryTag tag -> dynamicOps.createString(tag.value());
            case ListBinaryTag nbtList -> this.convertList(dynamicOps, nbtList);
            case CompoundBinaryTag nbtCompound -> this.convertMap(dynamicOps, nbtCompound);
            case IntArrayBinaryTag nbtIntArray -> dynamicOps.createIntList(Arrays.stream(nbtIntArray.value()));
            case LongArrayBinaryTag nbtLongArray -> dynamicOps.createLongList(Arrays.stream(nbtLongArray.value()));
            default -> throw new IllegalStateException("Unexpected value: " + binaryTag);
        };
    }

    public DataResult<Number> getNumberValue(BinaryTag binaryTag) {
        if (binaryTag instanceof NumberBinaryTag tag) {
            return DataResult.success(tag.numberValue());
        }

        return DataResult.error(() -> "Not a number");
    }

    public BinaryTag createNumeric(Number value) {
        return DoubleBinaryTag.doubleBinaryTag(value.doubleValue());
    }

    public BinaryTag createByte(byte value) {
        return ByteBinaryTag.byteBinaryTag(value);
    }

    public BinaryTag createShort(short value) {
        return ShortBinaryTag.shortBinaryTag(value);
    }

    public BinaryTag createInt(int value) {
        return IntBinaryTag.intBinaryTag(value);
    }

    public BinaryTag createLong(long value) {
        return LongBinaryTag.longBinaryTag(value);
    }

    public BinaryTag createFloat(float value) {
        return FloatBinaryTag.floatBinaryTag(value);
    }

    public BinaryTag createDouble(double value) {
        return DoubleBinaryTag.doubleBinaryTag(value);
    }

    public BinaryTag createBoolean(boolean value) {
        return createByte((byte) (value ? 1 : 0));
    }

    public DataResult<String> getStringValue(BinaryTag tag) {
        if (tag instanceof StringBinaryTag stringTag) {
            return DataResult.success(stringTag.value());
        } else {
            return DataResult.error(() -> "Not a string");
        }
    }

    public BinaryTag createString(String string) {
        return StringBinaryTag.stringBinaryTag(string);
    }

    public DataResult<BinaryTag> mergeToList(BinaryTag binaryTag, BinaryTag binaryTag2) {
        return createMerger(binaryTag).map((merger) -> DataResult.success(merger.merge(binaryTag2).getResult())).orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + binaryTag, binaryTag));
    }

    public DataResult<BinaryTag> mergeToList(BinaryTag binaryTag, List<BinaryTag> list) {
        return createMerger(binaryTag).map((merger) -> DataResult.success(merger.merge(list).getResult())).orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + binaryTag, binaryTag));
    }

    public DataResult<BinaryTag> mergeToMap(BinaryTag binaryTag, BinaryTag binaryTag2, BinaryTag binaryTag3) {
        if (!(binaryTag instanceof CompoundBinaryTag) && !(binaryTag instanceof EndBinaryTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + binaryTag, binaryTag);
        } else if (binaryTag2 instanceof StringBinaryTag var5) {
            String string;
            try {
                string = var5.value();
            } catch (Throwable var7) {
                throw new MatchException(var7.toString(), var7);
            }

            CompoundBinaryTag var11;
            if (binaryTag instanceof CompoundBinaryTag nbtCompound) {
                var11 = NbtHelpers.shallowCopy(nbtCompound);
            } else {
                var11 = CompoundBinaryTag.empty();
            }

            CompoundBinaryTag nbtCompound2 = var11;
            nbtCompound2.put(string, binaryTag3);
            return DataResult.success(nbtCompound2);
        } else {
            return DataResult.error(() -> "key is not a string: " + binaryTag2, binaryTag);
        }
    }

    public DataResult<BinaryTag> mergeToMap(BinaryTag BinaryTag, MapLike<BinaryTag> mapLike) {
        if (!(BinaryTag instanceof CompoundBinaryTag) && !(BinaryTag instanceof EndBinaryTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + BinaryTag, BinaryTag);
        } else {
            CompoundBinaryTag var10000;
            if (BinaryTag instanceof CompoundBinaryTag nbtCompound) {
                var10000 = NbtHelpers.shallowCopy(nbtCompound);
            } else {
                var10000 = CompoundBinaryTag.empty();
            }

            CompoundBinaryTag nbtCompound2 = var10000;
            List<BinaryTag> list = new ArrayList<>();
            mapLike.entries().forEach((pair) -> {
                BinaryTag binaryTag = pair.getFirst();
                if (binaryTag instanceof StringBinaryTag tag) {
                    nbtCompound2.put(tag.value(), pair.getSecond());
                } else {
                    list.add(binaryTag);
                }
            });
            return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, nbtCompound2) : DataResult.success(nbtCompound2);
        }
    }

    public DataResult<BinaryTag> mergeToMap(BinaryTag BinaryTag, Map<BinaryTag, BinaryTag> map) {
        if (!(BinaryTag instanceof CompoundBinaryTag) && !(BinaryTag instanceof EndBinaryTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + BinaryTag, BinaryTag);
        } else {
            CompoundBinaryTag var10000;
            if (BinaryTag instanceof CompoundBinaryTag nbtCompound) {
                var10000 = NbtHelpers.shallowCopy(nbtCompound);
            } else {
                var10000 = CompoundBinaryTag.empty();
            }

            CompoundBinaryTag nbtCompound2 = var10000;
            List<BinaryTag> list = new ArrayList<>();

            for(Map.Entry<BinaryTag, BinaryTag> entry : map.entrySet()) {
                BinaryTag binaryTag2 = entry.getKey();
                if (binaryTag2 instanceof StringBinaryTag tag) {
                    String string;
                    try {
                        string = tag.value();
                    } catch (Throwable var11) {
                        throw new MatchException(var11.toString(), var11);
                    }

                    nbtCompound2.put(string, entry.getValue());
                } else {
                    list.add(binaryTag2);
                }
            }

            if (!list.isEmpty()) {
                return DataResult.error(() -> "some keys are not strings: " + list, nbtCompound2);
            } else {
                return DataResult.success(nbtCompound2);
            }
        }
    }

    public DataResult<Stream<Pair<BinaryTag, BinaryTag>>> getMapValues(BinaryTag BinaryTag) {
        if (BinaryTag instanceof CompoundBinaryTag nbtCompound) {
            return DataResult.success(NbtHelpers.entrySet(nbtCompound).entrySet().stream().map((entry) -> Pair.of(this.createString(entry.getKey()), entry.getValue())));
        } else {
            return DataResult.error(() -> "Not a map: " + BinaryTag);
        }
    }

    public DataResult<Consumer<BiConsumer<BinaryTag, BinaryTag>>> getMapEntries(BinaryTag BinaryTag) {
        if (BinaryTag instanceof CompoundBinaryTag nbtCompound) {
            return DataResult.success((biConsumer) -> {
                NbtHelpers.entrySet(nbtCompound).forEach((s, binaryTag) ->
                        biConsumer.accept(this.createString(s), binaryTag)
                );
            });
        } else {
            return DataResult.error(() -> "Not a map: " + BinaryTag);
        }
    }

    public DataResult<MapLike<BinaryTag>> getMap(BinaryTag BinaryTag) {
        if (BinaryTag instanceof final CompoundBinaryTag nbtCompound) {
            return DataResult.success(new MapLike<BinaryTag>() {
                @Nullable
                public BinaryTag get(BinaryTag BinaryTag) {
                    if (BinaryTag instanceof StringBinaryTag tag) {
                        return nbtCompound.get(tag.value());
                    } else {
                        throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + BinaryTag);
                    }
                }

                @Nullable
                public BinaryTag get(String string) {
                    return nbtCompound.get(string);
                }

                public Stream<Pair<BinaryTag, BinaryTag>> entries() {
                    return NbtHelpers.entrySet(nbtCompound).entrySet().stream().map((entry) -> Pair.of(NbtOps.this.createString(entry.getKey()), entry.getValue()));
                }

                public String toString() {
                    return "MapLike[" + nbtCompound + "]";
                }
            });
        } else {
            return DataResult.error(() -> "Not a map: " + BinaryTag);
        }
    }

    public BinaryTag createMap(Stream<Pair<BinaryTag, BinaryTag>> stream) {
        CompoundBinaryTag nbtCompound = CompoundBinaryTag.empty();
        stream.forEach((entry) -> {
            BinaryTag BinaryTag = entry.getFirst();
            BinaryTag BinaryTag2 = entry.getSecond();
            if (BinaryTag instanceof StringBinaryTag tag) {
                nbtCompound.put(tag.value(), BinaryTag2);
            } else {
                throw new UnsupportedOperationException("Cannot create map with non-string key: " + BinaryTag);
            }
        });
        return nbtCompound;
    }

    public DataResult<Stream<BinaryTag>> getStream(BinaryTag BinaryTag) {
        if (BinaryTag instanceof ListBinaryTag abstractNbtList) {
            return DataResult.success(abstractNbtList.stream());
        } else {
            return DataResult.error(() -> "Not a list");
        }
    }

    public DataResult<Consumer<Consumer<BinaryTag>>> getList(BinaryTag BinaryTag) {
        if (BinaryTag instanceof ListBinaryTag abstractNbtList) {
            Objects.requireNonNull(abstractNbtList);
            return DataResult.success(abstractNbtList::forEach);
        } else {
            return DataResult.error(() -> "Not a list: " + BinaryTag);
        }
    }

    public DataResult<ByteBuffer> getByteBuffer(BinaryTag binaryTag) {
        if (binaryTag instanceof ByteArrayBinaryTag nbtByteArray) {
            return DataResult.success(ByteBuffer.wrap(nbtByteArray.value()));
        } else {
            return DynamicOps.super.getByteBuffer(binaryTag);
        }
    }

    public BinaryTag createByteList(ByteBuffer byteBuffer) {
        ByteBuffer byteBuffer2 = byteBuffer.duplicate().clear();
        byte[] bs = new byte[byteBuffer.capacity()];
        byteBuffer2.get(0, bs, 0, bs.length);
        return ByteArrayBinaryTag.byteArrayBinaryTag(bs);
    }

    public DataResult<IntStream> getIntStream(BinaryTag BinaryTag) {
        if (BinaryTag instanceof IntArrayBinaryTag nbtIntArray) {
            return DataResult.success(Arrays.stream(nbtIntArray.value()));
        } else {
            return DynamicOps.super.getIntStream(BinaryTag);
        }
    }

    public BinaryTag createIntList(IntStream intStream) {
        return IntArrayBinaryTag.intArrayBinaryTag(intStream.toArray());
    }

    public DataResult<LongStream> getLongStream(BinaryTag BinaryTag) {
        if (BinaryTag instanceof LongArrayBinaryTag nbtLongArray) {
            return DataResult.success(Arrays.stream(nbtLongArray.value()));
        } else {
            return DynamicOps.super.getLongStream(BinaryTag);
        }
    }

    public BinaryTag createLongList(LongStream longStream) {
        return LongArrayBinaryTag.longArrayBinaryTag(longStream.toArray());
    }

    public BinaryTag createList(Stream<BinaryTag> stream) {
        return ListBinaryTag.from(stream.toList());
    }

    public BinaryTag remove(BinaryTag BinaryTag, String string) {
        if (BinaryTag instanceof CompoundBinaryTag nbtCompound) {
            CompoundBinaryTag nbtCompound2 = NbtHelpers.shallowCopy(nbtCompound);
            nbtCompound2.remove(string);
            return nbtCompound2;
        } else {
            return BinaryTag;
        }
    }

    public String toString() {
        return "NBT";
    }

    public RecordBuilder<BinaryTag> mapBuilder() {
        return new MapBuilder();
    }

    private static Optional<Merger> createMerger(BinaryTag nbt) {
        switch (nbt) {
            case EndBinaryTag _ -> {
                return Optional.of(new CompoundListMerger());
            }
            case ListBinaryTag abstractNbtList -> {
                if (abstractNbtList.isEmpty()) {
                    return Optional.of(new CompoundListMerger());
                } else {
                    return Optional.of(new CompoundListMerger(abstractNbtList));
                }
            }
            case ArrayBinaryTag abstractNbtArray -> {
                switch (abstractNbtArray) {
                    case ByteArrayBinaryTag tag -> {
                        if (tag.size() == 0) {
                            return Optional.of(new CompoundListMerger());
                        } else {
                            return Optional.of(new ByteArrayMerger(tag.value()));
                        }
                    }
                    case IntArrayBinaryTag tag -> {
                        if (tag.size() == 0) {
                            return Optional.of(new CompoundListMerger());
                        } else {
                            return Optional.of(new IntArrayMerger(tag.value()));
                        }
                    }
                    case LongArrayBinaryTag tag -> {
                        if (tag.size() == 0) {
                            return Optional.of(new CompoundListMerger());
                        } else {
                            return Optional.of(new LongArrayMerger(tag.value()));
                        }
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + abstractNbtArray);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + nbt);
        }
    }

    class MapBuilder extends RecordBuilder.AbstractStringBuilder<BinaryTag, CompoundBinaryTag> {
        protected MapBuilder() {
            super(NbtOps.this);
        }

        protected CompoundBinaryTag initBuilder() {
            return CompoundBinaryTag.empty();
        }

        protected CompoundBinaryTag append(String string, BinaryTag BinaryTag, CompoundBinaryTag nbtCompound) {
            nbtCompound.put(string, BinaryTag);
            return nbtCompound;
        }

        protected DataResult<BinaryTag> build(CompoundBinaryTag nbtCompound, BinaryTag BinaryTag) {
            if (BinaryTag != null && BinaryTag != EndBinaryTag.endBinaryTag()) {
                if (!(BinaryTag instanceof CompoundBinaryTag nbtCompound2)) {
                    return DataResult.error(() -> "mergeToMap called with not a map: " + BinaryTag, BinaryTag);
                } else {
                    CompoundBinaryTag nbtCompound3 = NbtHelpers.shallowCopy(nbtCompound2);
                    NbtHelpers.entrySet(nbtCompound).forEach(nbtCompound3::put);
                    return DataResult.success(nbtCompound3);
                }
            } else {
                return DataResult.success(nbtCompound);
            }
        }
    }

    interface Merger {
        Merger merge(BinaryTag nbt);

        default Merger merge(Iterable<BinaryTag> nbts) {
            Merger merger = this;

            for(BinaryTag BinaryTag : nbts) {
                merger = merger.merge(BinaryTag);
            }

            return merger;
        }

        default Merger merge(Stream<BinaryTag> nbts) {
            Objects.requireNonNull(nbts);
            return this.merge(nbts::iterator);
        }

        BinaryTag getResult();
    }

    static class CompoundListMerger implements Merger {
        private final ListBinaryTag list = ListBinaryTag.from(new ArrayList<>());

        CompoundListMerger() {
        }

        CompoundListMerger(ListBinaryTag nbtList) {
            nbtList.forEach(this.list::add);
        }

        public CompoundListMerger(IntArrayList list) {
            list.forEach((value) -> this.list.add(IntBinaryTag.intBinaryTag(value)));
        }

        public CompoundListMerger(ByteArrayList list) {
            list.forEach((value) -> this.list.add(ByteBinaryTag.byteBinaryTag(value)));
        }

        public CompoundListMerger(LongArrayList list) {
            list.forEach((value) -> this.list.add(LongBinaryTag.longBinaryTag(value)));
        }

        public Merger merge(BinaryTag nbt) {
            this.list.add(nbt);
            return this;
        }

        public BinaryTag getResult() {
            return this.list;
        }
    }

    static class IntArrayMerger implements Merger {
        private final IntArrayList list = new IntArrayList();

        public IntArrayMerger(int[] values) {
            this.list.addElements(0, values);
        }

        public Merger merge(BinaryTag nbt) {
            if (nbt instanceof IntBinaryTag nbtInt) {
                this.list.add(nbtInt.intValue());
                return this;
            } else {
                return (new CompoundListMerger(this.list)).merge(nbt);
            }
        }

        public BinaryTag getResult() {
            return IntArrayBinaryTag.intArrayBinaryTag(this.list.toIntArray());
        }
    }

    static class ByteArrayMerger implements Merger {
        private final ByteArrayList list = new ByteArrayList();

        public ByteArrayMerger(byte[] values) {
            this.list.addElements(0, values);
        }

        public Merger merge(BinaryTag nbt) {
            if (nbt instanceof ByteBinaryTag nbtByte) {
                this.list.add(nbtByte.byteValue());
                return this;
            } else {
                return (new CompoundListMerger(this.list)).merge(nbt);
            }
        }

        public BinaryTag getResult() {
            return ByteArrayBinaryTag.byteArrayBinaryTag(this.list.toByteArray());
        }
    }

    static class LongArrayMerger implements Merger {
        private final LongArrayList list = new LongArrayList();

        public LongArrayMerger(long[] values) {
            this.list.addElements(0, values);
        }

        public Merger merge(BinaryTag nbt) {
            if (nbt instanceof LongBinaryTag nbtLong) {
                this.list.add(nbtLong.longValue());
                return this;
            } else {
                return (new CompoundListMerger(this.list)).merge(nbt);
            }
        }

        public BinaryTag getResult() {
            return LongArrayBinaryTag.longArrayBinaryTag(this.list.toLongArray());
        }
    }
}
