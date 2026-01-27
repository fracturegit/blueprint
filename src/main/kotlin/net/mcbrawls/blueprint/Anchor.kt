package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.nativePair
import java.util.Optional

/**
 * An entity-like point within a blueprint that can hold custom data.
 */
data class Anchor(
    val position: Vec3d,
    val rotation: Vec2f,
    val data: Optional<String> = Optional.empty(),
) {
    companion object {
        val CODEC: Codec<Anchor> = RecordCodecBuilder.create { instance ->
            instance.group(
                Vec3d.CODEC.fieldOf("position").forGetter(Anchor::position),
                Vec2f.CODEC.fieldOf("rotation").forGetter(Anchor::rotation),
                Codec.STRING.optionalFieldOf("data").orElseGet(Optional<String>::empty).forGetter(Anchor::data),
            ).apply(instance, ::Anchor)
        }

        val LEGACY_LIST_CODEC: Codec<List<Pair<String, Anchor>>> = Codec.withAlternative(
            nativePair(Codec.STRING.fieldOf("id").codec(), CODEC).listOf(),
            Codec.unboundedMap(Codec.STRING, CODEC).xmap({ it.toList() }, { it.toMap() })
        )
    }
}
