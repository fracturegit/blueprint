package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector2fc
import org.joml.Vector3dc

/**
 * A positional point within a blueprint: a location and facing direction.
 * Belongs to a [Marker]; use [Marker] to attach type info and group-level structured properties.
 *
 * Per-anchor [properties] allow individual points within a group to carry their own data -
 * e.g. `init_index` on specific spawns, `radius` or `dom_zone` on individual hills.
 */
data class Anchor(
    val position: Vector3dc,
    val rotation: Vector2fc,
    val properties: Map<String, PropertyValue> = emptyMap(),
) {
    companion object {
        val CODEC: Codec<Anchor> = RecordCodecBuilder.create { instance ->
            instance.group(
                VectorCodecs.VECTOR_3D.fieldOf("position").forGetter(Anchor::position),
                VectorCodecs.VECTOR_2F.fieldOf("rotation").forGetter(Anchor::rotation),
                PropertyValue.MAP_CODEC.optionalFieldOf("properties", emptyMap()).forGetter(Anchor::properties),
            ).apply(instance, ::Anchor)
        }
    }
}
