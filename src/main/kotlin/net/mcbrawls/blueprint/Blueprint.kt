package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.nativePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

data class Blueprint<T>(
    val palette: List<T>,
    val palettedStates: List<PalettedState>,
    val anchors: List<Pair<String, Anchor>>,
) {
    /**
     * The size of the blueprint.
     */
    val size: Vec3i = calculateBlueprintSize(palettedStates.map(PalettedState::pos))

    fun forEach(action: (Vec3i, T) -> Unit) {
        palettedStates.forEach { (offset, index) ->
            val state = palette[index]
            action.invoke(offset, state)
        }
    }

    fun forEachPosition(action: (Vec3i) -> Unit) {
        palettedStates.map(PalettedState::pos).forEach(action)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(Blueprint::class.java)

        /**
         * The codec of this class.
         */
        fun <T> createCodec(to: (State) -> T, from: (T) -> State): Codec<Blueprint<T>> = RecordCodecBuilder.create { instance ->
            instance.group(
                State.CODEC
                    .orElse(Consumer { error ->
                        logger.error("Could not load blockstate: $error")
                    }, State.empty)
                    .xmap(to, from)
                    .listOf()
                    .fieldOf("palette")
                    .forGetter(Blueprint<T>::palette),
                PalettedState.CODEC.listOf()
                    .fieldOf("block_states")
                    .forGetter(Blueprint<T>::palettedStates),
                /*BlueprintBlockEntity.CODEC.listOf()
                    .fieldOf("block_entities")
                    .xmap({ entry -> entry.associateBy(BlueprintBlockEntity::blockPos) }, { map -> map.values.toList() })
                    .orElse(emptyMap())
                    .forGetter(Blueprint::blockEntities),*/
                /*Codec.unboundedMap(Codec.STRING, SerializableRegion.CODEC)
                    .fieldOf("regions")
                    .orElse(emptyMap())
                    .forGetter(Blueprint::regions),*/
                Codec.withAlternative(
                    nativePair(Codec.STRING.fieldOf("id").codec(), Anchor.CODEC).listOf(),
                    Codec.unboundedMap(Codec.STRING, Anchor.CODEC).xmap({ it.toList() }, { it.toMap() })
                )
                    .fieldOf("anchors")
                    .orElse(emptyList())
                    .forGetter(Blueprint<T>::anchors),
            ).apply(instance, ::Blueprint)
        }

        /**
         * Calculates the size of a blueprint from its positions.
         * @return the blueprint size
         */
        fun calculateBlueprintSize(positions: List<Vec3i>): Vec3i {
            if (positions.isEmpty()) {
                return Vec3i.ZERO
            }

            val minX = positions.minOf { it.x }
            val minY = positions.minOf { it.y }
            val minZ = positions.minOf { it.z }

            val maxX = positions.maxOf { it.x }
            val maxY = positions.maxOf { it.y }
            val maxZ = positions.maxOf { it.z }

            return Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1)
        }
    }
}
