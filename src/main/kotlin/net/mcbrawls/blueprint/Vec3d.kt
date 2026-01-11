package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult

data class Vec3d(val x: Double, val y: Double, val z: Double) {
    fun add(position: Vec3i): Vec3d {
        return Vec3d(x + position.x, y + position.y, z + position.z)
    }

    operator fun plus(other: Vec3d) = Vec3d(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3d) = Vec3d(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double) = Vec3d(x * scalar, y * scalar, z * scalar)

    companion object {
        val CODEC: Codec<Vec3d> = Codec.DOUBLE.listOf().comapFlatMap(
            { stream ->
                val list = stream.toList()
                if (list.size == 3) {
                    DataResult.success(Vec3d(list[0], list[1], list[2]))
                } else {
                    DataResult.error { "Expected 3 ints for Vec3i, got ${list.size}" }
                }
            },
            { vec -> listOf(vec.x, vec.y, vec.z) }
        )
    }
}
