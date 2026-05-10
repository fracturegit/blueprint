package net.mcbrawls.blueprint

import org.joml.Vector3i

// Ordinal order is CW rotation cycle (NORTH=0, EAST=1, SOUTH=2, WEST=3)
// so that opposite() and Rotation.rotate() formulas work correctly.
enum class CardinalDirection {
    NORTH, EAST, SOUTH, WEST;

    fun opposite(): CardinalDirection = entries[(ordinal + 2) % 4]

    val step: Vector3i get() = when (this) {
        NORTH -> Vector3i(0, 0, -1)
        SOUTH -> Vector3i(0, 0, 1)
        EAST  -> Vector3i(1, 0, 0)
        WEST  -> Vector3i(-1, 0, 0)
    }
}
