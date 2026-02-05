package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import net.mcbrawls.codex.nativePair
import org.joml.Vector2fc
import org.joml.Vector3dc
import java.util.Optional

/**
 * An entity-like point within a blueprint that can hold custom data.
 */
data class Anchor(
    val position: Vector3dc,
    val rotation: Vector2fc,
    val data: Optional<String> = Optional.empty(),
) {
    companion object {
        val CODEC: Codec<Anchor> = RecordCodecBuilder.create { instance ->
            instance.group(
                VectorCodecs.VECTOR_3D.fieldOf("position").forGetter(Anchor::position),
                VectorCodecs.VECTOR_2F.fieldOf("rotation").forGetter(Anchor::rotation),
                Codec.STRING.optionalFieldOf("data").forGetter(Anchor::data),
            ).apply(instance, ::Anchor)
        }

        val LEGACY_LIST_CODEC: Codec<List<Pair<String, Anchor>>> = Codec.withAlternative(
            nativePair(Codec.STRING.fieldOf("id").codec(), CODEC).listOf(),
            Codec.unboundedMap(Codec.STRING, CODEC).xmap({ it.toList() }, { it.toMap() })
        )
    }
}
