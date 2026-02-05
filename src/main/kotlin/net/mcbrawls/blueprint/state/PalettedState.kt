package net.mcbrawls.blueprint.state

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector3ic

data class PalettedState(
    /**
     * The offset position from the root of the blueprint placement.
     */
    val pos: Vector3ic,

    /**
     * The index of the block state in the blueprint's palette.
     */
    val paletteIndex: Int,
) {
    override fun toString(): String {
        return "PalettedState[#$paletteIndex, $pos]"
    }

    companion object {
        /**
         * The codec of a paletted state.
         */
        val CODEC: Codec<PalettedState> = RecordCodecBuilder.create { instance ->
            instance.group(
                VectorCodecs.VECTOR_3I.fieldOf("offset").forGetter(PalettedState::pos),
                Codec.INT.fieldOf("index").forGetter(PalettedState::paletteIndex)
            ).apply(instance, ::PalettedState)
        }
    }
}
