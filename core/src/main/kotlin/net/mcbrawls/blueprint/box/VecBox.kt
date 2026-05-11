package net.mcbrawls.blueprint.box

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.Rotation
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.joml.minus
import org.joml.plus

class VecBox(a: Vector3dc, b: Vector3dc) : Box<Vector3dc, Double, VecBox>() {
    override val min: Vector3dc
    override val max: Vector3dc
    override val center: Vector3dc
    override val size: Double

    init {
        val (min, max) = minMax(a, b)
        this.min = min
        this.max = max
        this.center = min + (max - min) / 2.0
        this.size = (max.x() - min.x() + 1) * (max.y() - min.y() + 1) * (max.z() - min.z() + 1)
    }

    /**
     * Returns a new VecBox whose corners have been rotated about the blueprint origin,
     * then component-wise min/max re-derived so the result is always axis-aligned.
     */
    fun rotated(rotation: Rotation, blueprintSize: Vector3ic): VecBox {
        val rMin = rotation.rotateVec3d(min, blueprintSize)
        val rMax = rotation.rotateVec3d(max, blueprintSize)
        return VecBox(
            Vector3d(minOf(rMin.x, rMax.x), minOf(rMin.y, rMax.y), minOf(rMin.z, rMax.z)),
            Vector3d(maxOf(rMin.x, rMax.x), maxOf(rMin.y, rMax.y), maxOf(rMin.z, rMax.z)),
        )
    }

    override fun offset(vec: Vector3dc): VecBox {
        val newMin = min + vec
        val newMax = max + vec
        return VecBox(newMin, newMax)
    }

    override fun contains(x: Double, y: Double, z: Double): Boolean {
        return x in min.x()..max.x() &&
                y in min.y()..max.y() &&
                z in min.z()..max.z()
    }

    override fun contains(vec: Vector3dc): Boolean {
        return contains(vec.x(), vec.y(), vec.z())
    }

    override fun toString(): String {
        return "VecBox{$min, $max}"
    }

    companion object {
        val CODEC: Codec<VecBox> = RecordCodecBuilder.create { instance ->
            instance.group(
                VectorCodecs.VECTOR_3D.fieldOf("min").forGetter(VecBox::min),
                VectorCodecs.VECTOR_3D.fieldOf("max").forGetter(VecBox::max),
            ).apply(instance, ::VecBox)
        }

        fun minMax(a: Vector3dc, b: Vector3dc): Pair<Vector3dc, Vector3dc> {
            val min = Vector3d(
                minOf(a.x(), b.x()),
                minOf(a.y(), b.y()),
                minOf(a.z(), b.z())
            )
            val max = Vector3d(
                maxOf(a.x(), b.x()),
                maxOf(a.y(), b.y()),
                maxOf(a.z(), b.z())
            )
            return min to max
        }
    }
}
