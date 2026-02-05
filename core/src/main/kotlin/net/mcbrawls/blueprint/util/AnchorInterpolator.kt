package net.mcbrawls.blueprint.util

import net.mcbrawls.blueprint.Anchor
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.minus
import org.joml.plus
import org.joml.times

class AnchorInterpolator(private val anchors: List<Anchor>) {
    init {
        require(anchors.size >= 2) { "At least 2 anchors are required for interpolation" }
    }

    /**
     * Smooth Catmull-Rom spline interpolation through all anchors
     * @param progress Value between 0.0 (first anchor) and 1.0 (last anchor)
     * Creates smooth curves that pass through all anchors
     */
    fun interpolate(progress: Float): Anchor {
        val clampedProgress = progress.coerceIn(0f, 1f)

        if (anchors.size == 2) {
            return interpolateLinear(clampedProgress)
        }

        if (clampedProgress == 0f) return anchors.first()
        if (clampedProgress == 1f) return anchors.last()

        val segmentCount = anchors.size - 1
        val segmentProgress = clampedProgress * segmentCount
        val segmentIndex = segmentProgress.toInt().coerceIn(0, segmentCount - 1)
        val t = segmentProgress - segmentIndex

        // Get control points for Catmull-Rom spline
        val p0 = anchors.getOrNull(segmentIndex - 1) ?: anchors[segmentIndex]
        val p1 = anchors[segmentIndex]
        val p2 = anchors[segmentIndex + 1]
        val p3 = anchors.getOrNull(segmentIndex + 2) ?: anchors[segmentIndex + 1]

        val position = catmullRomVec3d(p0.position, p1.position, p2.position, p3.position, t)
        val rotation = catmullRomVec2f(p0.rotation, p1.rotation, p2.rotation, p3.rotation, t)

        return Anchor(position, rotation)
    }

    /**
     * Linear interpolation through all anchors (no smoothing)
     */
    fun interpolateLinear(progress: Float): Anchor {
        val clampedProgress = progress.coerceIn(0f, 1f)

        if (clampedProgress == 0f) return anchors.first()
        if (clampedProgress == 1f) return anchors.last()

        val segmentCount = anchors.size - 1
        val segmentProgress = clampedProgress * segmentCount
        val segmentIndex = segmentProgress.toInt().coerceIn(0, segmentCount - 1)
        val t = segmentProgress - segmentIndex

        val startAnchor = anchors[segmentIndex]
        val endAnchor = anchors[segmentIndex + 1]

        val positionDelta = endAnchor.position - startAnchor.position
        val interpolatedPosition = startAnchor.position + (positionDelta * t.toDouble())

        val rotationDelta = endAnchor.rotation - startAnchor.rotation
        val interpolatedRotation = startAnchor.rotation + (rotationDelta * t)

        return Anchor(interpolatedPosition, interpolatedRotation)
    }

    private fun catmullRomVec3d(p0: Vector3dc, p1: Vector3dc, p2: Vector3dc, p3: Vector3dc, t: Float): Vector3d {
        val t2 = t * t
        val t3 = t2 * t
        val td = t.toDouble()
        val t2d = t2.toDouble()
        val t3d = t3.toDouble()

        return (p0 * (-t3d + 2.0 * t2d - td) +
                p1 * (3.0 * t3d - 5.0 * t2d + 2.0) +
                p2 * (-3.0 * t3d + 4.0 * t2d + td) +
                p3 * (t3d - t2d)) * 0.5
    }

    private fun catmullRomVec2f(p0: Vector2fc, p1: Vector2fc, p2: Vector2fc, p3: Vector2fc, t: Float): Vector2f {
        // Adjust each point to be on the shortest path from the previous one
        val p1Adjusted = adjustAngle(p0, p1)
        val p2Adjusted = adjustAngle(p1Adjusted, p2)
        val p3Adjusted = adjustAngle(p2Adjusted, p3)

        val t2 = t * t
        val t3 = t2 * t

        return (p0 * (-t3 + 2f * t2 - t) +
                p1Adjusted * (3f * t3 - 5f * t2 + 2f) +
                p2Adjusted * (-3f * t3 + 4f * t2 + t) +
                p3Adjusted * (t3 - t2)) * 0.5f
    }

    private fun adjustAngle(from: Vector2fc, to: Vector2fc): Vector2f {
        return Vector2f(
            adjustSingleAngle(from.x(), to.x()),
            adjustSingleAngle(from.y(), to.y())
        )
    }

    private fun adjustSingleAngle(from: Float, to: Float): Float {
        var adjusted = to
        while (adjusted - from > 180f) adjusted -= 360f
        while (adjusted - from < -180f) adjusted += 360f
        return adjusted
    }
}
