package net.mcbrawls.blueprint

import org.joml.Vector3i

// Ordinal order is CW rotation cycle (NORTH=0, EAST=1, SOUTH=2, WEST=3)
// so that opposite() and Rotation.rotate() formulas work correctly.
enum class CardinalDirection(
    val dx: Int,
    val dz: Int,
    val rightX: Int,
    val rightZ: Int,
) {
    NORTH(0, -1, 1, 0),
    EAST(1, 0, 0, 1),
    SOUTH(0, 1, -1, 0),
    WEST(-1, 0, 0, -1);

    fun opposite(): CardinalDirection = entries[(ordinal + 2) % 4]

    val step: Vector3i = Vector3i(dx, 0, dz)
}
