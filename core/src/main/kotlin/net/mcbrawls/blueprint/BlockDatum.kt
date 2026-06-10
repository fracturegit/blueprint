package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector3ic

data class BlockDatum(
    val position: Vector3ic,
    val properties: Map<String, PropertyValue> = emptyMap(),
) {
    fun getBoolean(key: String): Boolean? = (properties[key] as? PropertyValue.BoolValue)?.value
    fun getInt(key: String): Int? = (properties[key] as? PropertyValue.IntValue)?.value
    fun getDouble(key: String): Double? = (properties[key] as? PropertyValue.DoubleValue)?.value
    fun getString(key: String): String? = (properties[key] as? PropertyValue.StringValue)?.value
    operator fun contains(key: String): Boolean = key in properties

    companion object {
        val CODEC: Codec<BlockDatum> = RecordCodecBuilder.create { instance ->
            instance.group(
                VectorCodecs.VECTOR_3I.fieldOf("position").forGetter(BlockDatum::position),
                PropertyValue.MAP_CODEC.optionalFieldOf("properties", emptyMap()).forGetter(BlockDatum::properties),
            ).apply(instance, ::BlockDatum)
        }
    }
}
