package net.mcbrawls.blueprint

import org.joml.Vector3i
import org.joml.Vector3ic

enum class Rotation(val turns: Int) {
    NONE(0), CW_90(1), CW_180(2), CW_270(3);

    fun rotate(direction: CardinalDirection): CardinalDirection =
        CardinalDirection.entries[(direction.ordinal + turns) % 4]

    fun rotatedSize(original: Vector3ic): Vector3ic = when (this) {
        NONE, CW_180 -> Vector3i(original.x(), original.y(), original.z())
        CW_90, CW_270 -> Vector3i(original.z(), original.y(), original.x())
    }
}
