package net.mcbrawls.blueprint.util

import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.minus
import org.joml.plus
import org.joml.times

object SplineMath {
    fun catmullRomVec3d(p0: Vector3dc, p1: Vector3dc, p2: Vector3dc, p3: Vector3dc, t: Float): Vector3d {
        val t2 = t * t; val t3 = t2 * t
        val td = t.toDouble(); val t2d = t2.toDouble(); val t3d = t3.toDouble()
        return (p0 * (-t3d + 2.0 * t2d - td) +
                p1 * (3.0 * t3d - 5.0 * t2d + 2.0) +
                p2 * (-3.0 * t3d + 4.0 * t2d + td) +
                p3 * (t3d - t2d)) * 0.5
    }

    fun catmullRomVec2f(p0: Vector2fc, p1: Vector2fc, p2: Vector2fc, p3: Vector2fc, t: Float): Vector2f {
        val p1a = adjustAngle(p0, p1)
        val p2a = adjustAngle(p1a, p2)
        val p3a = adjustAngle(p2a, p3)
        val t2 = t * t; val t3 = t2 * t
        return (p0 * (-t3 + 2f * t2 - t) +
                p1a * (3f * t3 - 5f * t2 + 2f) +
                p2a * (-3f * t3 + 4f * t2 + t) +
                p3a * (t3 - t2)) * 0.5f
    }

    fun lerpVec3d(a: Vector3dc, b: Vector3dc, t: Float): Vector3d {
        val delta = b - a
        return a + (delta * t.toDouble())
    }

    fun lerpVec2f(a: Vector2fc, b: Vector2fc, t: Float): Vector2f {
        val delta = b - a
        return a + (delta * t)
    }

    private fun adjustAngle(from: Vector2fc, to: Vector2fc) = Vector2f(
        adjustSingle(from.x(), to.x()),
        adjustSingle(from.y(), to.y())
    )

    private fun adjustSingle(from: Float, to: Float): Float {
        var v = to
        while (v - from > 180f) v -= 360f
        while (v - from < -180f) v += 360f
        return v
    }
}
