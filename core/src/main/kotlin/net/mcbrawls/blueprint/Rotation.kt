package net.mcbrawls.blueprint

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic

enum class Rotation(val turns: Int) {
    NONE(0), CW_90(1), CW_180(2), CW_270(3);

    fun rotate(direction: CardinalDirection): CardinalDirection =
        CardinalDirection.entries[(direction.ordinal + turns) % 4]

    /**
     * Rotates a continuous (floating-point) local position within a blueprint of [size].
     * The pivot is the blueprint origin (0, 0, 0).
     * Accepts the read-only [Vector3dc] interface so it works with both VecBox corners and Anchor positions.
     */
    fun rotateVec3d(v: Vector3dc, size: Vector3ic): Vector3d {
        val sx = size.x().toDouble()
        val sz = size.z().toDouble()
        return when (this) {
            NONE   -> Vector3d(v.x(), v.y(), v.z())
            CW_90  -> Vector3d(sz - v.z(), v.y(), v.x())
            CW_180 -> Vector3d(sx - v.x(), v.y(), sz - v.z())
            CW_270 -> Vector3d(v.z(),      v.y(), sx - v.x())
        }
    }

    /**
     * Rotates an integer (block) local position within a blueprint of [size].
     * The pivot is the blueprint origin (0, 0, 0).
     */
    fun rotateVec3i(v: Vector3ic, size: Vector3ic): Vector3i {
        val sx = size.x()
        val sz = size.z()
        return when (this) {
            NONE   -> Vector3i(v.x(), v.y(), v.z())
            CW_90  -> Vector3i(sz - 1 - v.z(), v.y(), v.x())
            CW_180 -> Vector3i(sx - 1 - v.x(), v.y(), sz - 1 - v.z())
            CW_270 -> Vector3i(v.z(),           v.y(), sx - 1 - v.x())
        }
    }

    /**
     * Rotates a yaw angle (degrees) by the number of clockwise 90° turns.
     */
    fun rotateYaw(yaw: Float): Float = (yaw + turns * 90f) % 360f

    fun rotatedSize(original: Vector3ic): Vector3ic = when (this) {
        NONE, CW_180  -> Vector3i(original.x(), original.y(), original.z())
        CW_90, CW_270 -> Vector3i(original.z(), original.y(), original.x())
    }
}
