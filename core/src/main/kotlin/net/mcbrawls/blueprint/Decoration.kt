package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.kyori.adventure.key.Key
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector2fc
import org.joml.Vector3dc

data class Decoration(
    val type: Key,
    val position: Vector3dc,
    val rotation: Vector2fc,
    val properties: Map<String, PropertyValue> = emptyMap(),
) {
    companion object {
        val CODEC: Codec<Decoration> = RecordCodecBuilder.create { instance ->
            instance.group(
                Marker.KEY_CODEC.fieldOf("type").forGetter(Decoration::type),
                VectorCodecs.VECTOR_3D.fieldOf("position").forGetter(Decoration::position),
                VectorCodecs.VECTOR_2F.fieldOf("rotation").forGetter(Decoration::rotation),
                PropertyValue.MAP_CODEC.optionalFieldOf("properties", emptyMap()).forGetter(Decoration::properties),
            ).apply(instance, ::Decoration)
        }
    }
}
