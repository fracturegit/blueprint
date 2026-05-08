package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector2fc
import org.joml.Vector3dc

data class Waypoint(
    val position: Vector3dc,
    val rotation: Vector2fc,
) {
    companion object {
        val CODEC: Codec<Waypoint> = RecordCodecBuilder.create { instance ->
            instance.group(
                VectorCodecs.VECTOR_3D.fieldOf("position").forGetter(Waypoint::position),
                VectorCodecs.VECTOR_2F.fieldOf("rotation").forGetter(Waypoint::rotation),
            ).apply(instance, ::Waypoint)
        }
    }
}
