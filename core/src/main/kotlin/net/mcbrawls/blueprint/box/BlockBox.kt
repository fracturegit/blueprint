package net.mcbrawls.blueprint.box

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.minus
import org.joml.plus
import kotlin.math.floor

class BlockBox(a: Vector3ic, b: Vector3ic) : Box<Vector3ic, Int, BlockBox>() {
    override val min: Vector3ic
    override val max: Vector3ic
    override val center: Vector3ic
    override val size: Int

    init {
        val (min, max) = minMax(a, b)
        this.min = min
        this.max = max
        this.center = min + (max - min) / 2
        this.size = (max.x() - min.x() + 1) * (max.y() - min.y() + 1) * (max.z() - min.z() + 1)
    }

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

    override fun toString(): String {
        return "BlockBox{$min, $max}"
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

        fun of(box: VecBox): BlockBox {
            val min = box.min
            val max = box.max
            return BlockBox(
                Vector3i(
                    floor(min.x()).toInt(),
                    floor(min.y()).toInt(),
                    floor(min.z()).toInt(),
                ),
                Vector3i(
                    floor(max.x()).toInt(),
                    floor(max.y()).toInt(),
                    floor(max.z()).toInt(),
                ),
            )
        }
    }
}
