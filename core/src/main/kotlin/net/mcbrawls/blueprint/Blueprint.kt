package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.box.VecBox
import net.mcbrawls.blueprint.state.PalettedState
import net.mcbrawls.blueprint.state.State
import org.joml.Vector3i
import org.joml.Vector3ic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class Blueprint<T>(
    val palette: List<T>,
    val palettedStates: List<PalettedState>,
    val anchors: List<Pair<String, Anchor>>,
    val regions: Map<String, VecBox>,
) {
    /**
     * The size of the blueprint.
     */
    val size: Vector3ic = calculateBlueprintSize(palettedStates.map(PalettedState::pos))

    fun forEach(action: (Vector3ic, T) -> Unit) {
        palettedStates.forEach { (offset, index) ->
            val state = palette[index]
            action.invoke(offset, state)
        }
    }

    fun forEachPosition(action: (Vector3ic) -> Unit) {
        palettedStates.map(PalettedState::pos).forEach(action)
    }

    override fun toString(): String {
        return "Blueprint{${palette.size} unique, ${palettedStates.size} positions, ${anchors.size} anchors}"
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(Blueprint::class.java)

        /**
         * The codec of this class.
         */
        fun <T> createCodec(to: (State) -> T, from: (T) -> State): Codec<Blueprint<T>> = RecordCodecBuilder.create { instance ->
            instance.group(
                State.CODEC.xmap(to, from).listOf().fieldOf("palette").forGetter(Blueprint<T>::palette),
                PalettedState.CODEC.listOf().fieldOf("block_states").forGetter(Blueprint<T>::palettedStates),
                Anchor.LEGACY_LIST_CODEC.optionalFieldOf("anchors", emptyList()).forGetter(Blueprint<T>::anchors),
                Codec.unboundedMap(Codec.STRING, VecBox.CODEC).optionalFieldOf("regions", emptyMap()).forGetter(Blueprint<T>::regions),
            ).apply(instance, ::Blueprint)
        }

        /**
         * Calculates the size of a blueprint from its positions.
         * @return the blueprint size
         */
        fun calculateBlueprintSize(positions: List<Vector3ic>): Vector3i {
            if (positions.isEmpty()) {
                return Vector3i()
            }

            val minX = positions.minOf { it.x() }
            val minY = positions.minOf { it.y() }
            val minZ = positions.minOf { it.z() }

            val maxX = positions.maxOf { it.x() }
            val maxY = positions.maxOf { it.y() }
            val maxZ = positions.maxOf { it.z() }

            return Vector3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1)
        }
    }
}
