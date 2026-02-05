package net.mcbrawls.blueprint.box

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.minus
import org.joml.plus

class VecBox(a: Vector3dc, b: Vector3dc) : Box<Vector3dc, Double, VecBox>() {
    override val min: Vector3dc
    override val max: Vector3dc

    init {
        val (min, max) = minMax(a, b)
        this.min = min
        this.max = max
    }

    override val center: Vector3dc = min + (max - min) / 2.0

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
