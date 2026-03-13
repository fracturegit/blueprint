package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Dynamic
import net.kyori.adventure.nbt.ByteBinaryTag
import net.kyori.adventure.nbt.DoubleBinaryTag
import net.kyori.adventure.nbt.IntBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag
import net.mcbrawls.blueprint.util.NbtOps

/**
 * A typed property value that can be stored on a [Marker] or [Anchor].
 */
sealed interface PropertyValue {
    val display: String

    @JvmInline value class IntValue(val value: Int) : PropertyValue {
        override val display get() = value.toString()
    }

    @JvmInline value class DoubleValue(val value: Double) : PropertyValue {
        override val display get() = value.toString()
    }

    @JvmInline value class StringValue(val value: String) : PropertyValue {
        override val display get() = "\"$value\""
    }

    @JvmInline value class BoolValue(val value: Boolean) : PropertyValue {
        override val display get() = value.toString()
    }

    companion object {
        val CODEC: Codec<PropertyValue> = Codec.PASSTHROUGH.comapFlatMap(
            { dynamic ->
                when (val tag = dynamic.convert(NbtOps.INSTANCE).value) {
                    is ByteBinaryTag   -> DataResult.success(BoolValue(tag.value() != 0.toByte()))
                    is IntBinaryTag    -> DataResult.success(IntValue(tag.value()))
                    is DoubleBinaryTag -> DataResult.success(DoubleValue(tag.value()))
                    is StringBinaryTag -> DataResult.success(StringValue(tag.value()))
                    else -> DataResult.error { "Unsupported NBT tag type for PropertyValue: ${tag::class.simpleName}" }
                }
            },
            { value ->
                when (value) {
                    is BoolValue   -> Dynamic(NbtOps.INSTANCE, ByteBinaryTag.byteBinaryTag(if (value.value) 1 else 0))
                    is IntValue    -> Dynamic(NbtOps.INSTANCE, IntBinaryTag.intBinaryTag(value.value))
                    is DoubleValue -> Dynamic(NbtOps.INSTANCE, DoubleBinaryTag.doubleBinaryTag(value.value))
                    is StringValue -> Dynamic(NbtOps.INSTANCE, StringBinaryTag.stringBinaryTag(value.value))
                }
            }
        )

        val MAP_CODEC: Codec<Map<String, PropertyValue>> = Codec.unboundedMap(Codec.STRING, CODEC)
    }
}
