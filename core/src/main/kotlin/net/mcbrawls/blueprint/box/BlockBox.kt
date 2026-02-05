package net.mcbrawls.blueprint.box

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.minus
import org.joml.plus

class BlockBox(a: Vector3ic, b: Vector3ic) : Box<Vector3ic, Int, BlockBox>() {
    override val min: Vector3ic
    override val max: Vector3ic

    init {
        val (min, max) = minMax(a, b)
        this.min = min
        this.max = max
    }

    override val center: Vector3ic = min + (max - min) / 2

    override fun offset(vec: Vector3ic): BlockBox {
        val newMin = min + vec
        val newMax = max + vec
        return BlockBox(newMin, newMax)
    }

    override fun contains(x: Int, y: Int, z: Int): Boolean {
        return x in min.x()..max.x() &&
                y in min.y()..max.y() &&
                z in min.z()..max.z()
    }

    override fun contains(vec: Vector3ic): Boolean {
        return contains(vec.x(), vec.y(), vec.z())
    }

    fun forEach(action: (Vector3ic) -> Unit) {
        (min.x()..max.x()).forEach { x ->
            (min.y()..max.y()).forEach { y ->
                (min.z()..max.z()).forEach { z ->
                    action.invoke(Vector3i(x, y, z))
                }
            }
        }
    }

    companion object {
        val CODEC: Codec<BlockBox> = RecordCodecBuilder.create { instance ->
            instance.group(
                VectorCodecs.VECTOR_3I.fieldOf("min").forGetter(BlockBox::min),
                VectorCodecs.VECTOR_3I.fieldOf("max").forGetter(BlockBox::max),
            ).apply(instance, ::BlockBox)
        }

        fun minMax(a: Vector3ic, b: Vector3ic): Pair<Vector3ic, Vector3ic> {
            val min = Vector3i(
                minOf(a.x(), b.x()),
                minOf(a.y(), b.y()),
                minOf(a.z(), b.z())
            )
            val max = Vector3i(
                maxOf(a.x(), b.x()),
                maxOf(a.y(), b.y()),
                maxOf(a.z(), b.z())
            )
            return min to max
        }
    }
}
