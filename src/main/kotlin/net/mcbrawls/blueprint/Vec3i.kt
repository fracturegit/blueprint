package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.util.stream.IntStream
import kotlin.streams.toList

data class Vec3i(val x: Int, val y: Int, val z: Int) {
    operator fun plus(vec: Vec3i): Vec3i {
        return Vec3i(x + vec.x, y + vec.y, z + vec.z)
    }

    operator fun minus(vec: Vec3i): Vec3i {
        return Vec3i(x - vec.x, y - vec.y, z - vec.z)
    }

    operator fun times(factor: Double): Vec3i {
        return Vec3i((x * factor).toInt(), (y * factor).toInt(), (z * factor).toInt())
    }

    companion object {
        val CODEC: Codec<Vec3i> = Codec.INT_STREAM.comapFlatMap(
            { stream ->
                val list = stream.toList()
                if (list.size == 3) {
                    DataResult.success(Vec3i(list[0], list[1], list[2]))
                } else {
                    DataResult.error { "Expected 3 ints for Vec3i, got ${list.size}" }
                }
            },
            { vec -> IntStream.of(vec.x, vec.y, vec.z) }
        )

        val ZERO = Vec3i(0, 0, 0)

        fun minMax(a: Vec3i, b: Vec3i): Pair<Vec3i, Vec3i> {
            val min = Vec3i(
                x = minOf(a.x, b.x),
                y = minOf(a.y, b.y),
                z = minOf(a.z, b.z)
            )
            val max = Vec3i(
                x = maxOf(a.x, b.x),
                y = maxOf(a.y, b.y),
                z = maxOf(a.z, b.z)
            )
            return min to max
        }
    }
}
