package net.mcbrawls.blueprint.serialization

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import java.util.stream.IntStream
import kotlin.streams.toList

object VectorCodecs {
    val VECTOR_2F: Codec<Vector2fc> = Codec.FLOAT.listOf(2, 2).xmap({ Vector2f(it[0], it[1]) }, { listOf(it.x(), it.y()) })

    val VECTOR_3D: Codec<Vector3dc> = Codec.DOUBLE.listOf().comapFlatMap(
        { stream ->
            val list = stream.toList()
            if (list.size == 3) {
                DataResult.success(Vector3d(list[0], list[1], list[2]))
            } else {
                DataResult.error { "Expected 3 doubles for Vec3i, got ${list.size}" }
            }
        },
        { vec -> listOf(vec.x(), vec.y(), vec.z()) }
    )

    val VECTOR_3I: Codec<Vector3ic> = Codec.INT_STREAM.comapFlatMap(
        { stream ->
            val list = stream.toList()
            if (list.size == 3) {
                DataResult.success(Vector3i(list[0], list[1], list[2]))
            } else {
                DataResult.error { "Expected 3 ints for Vec3i, got ${list.size}" }
            }
        },
        { vec -> IntStream.of(vec.x(), vec.y(), vec.z()) }
    )
}
