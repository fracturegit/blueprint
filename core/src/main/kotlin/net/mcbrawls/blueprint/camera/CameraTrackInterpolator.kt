package net.mcbrawls.blueprint.camera

import net.mcbrawls.blueprint.util.SplineMath
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3d
import org.joml.Vector3dc

class CameraTrackInterpolator(private val track: CameraTrack) {
    val totalDuration: Double = track.keyframes.drop(1).sumOf { it.duration }

    fun interpolate(timeSeconds: Double): Pair<Vector3dc, Vector2fc> {
        val keyframes = track.keyframes
        if (keyframes.isEmpty()) return Pair(Vector3d(), Vector2f())
        if (keyframes.size == 1) return Pair(keyframes[0].position, keyframes[0].rotation)

        val clampedTime = timeSeconds.coerceIn(0.0, totalDuration)
        var accumulated = 0.0

        for (i in 1 until keyframes.size) {
            val curr = keyframes[i]
            val prev = keyframes[i - 1]
            val segmentEnd = accumulated + curr.duration
            val isLastSegment = i == keyframes.size - 1

            if (clampedTime > segmentEnd && !isLastSegment) {
                accumulated = segmentEnd
                continue
            }

            if (curr.easing == EasingFunction.STEP) {
                return if (clampedTime >= segmentEnd) Pair(curr.position, curr.rotation)
                else Pair(prev.position, prev.rotation)
            }

            val rawT = if (curr.duration == 0.0) 1f
                       else ((clampedTime - accumulated) / curr.duration).toFloat().coerceIn(0f, 1f)
            val easedT = EasingFunctions.apply(curr.easing, rawT)

            return when (curr.pathMode) {
                PathMode.LINEAR -> Pair(
                    SplineMath.lerpVec3d(prev.position, curr.position, easedT),
                    SplineMath.lerpVec2f(prev.rotation, curr.rotation, easedT),
                )
                PathMode.CATMULL_ROM -> {
                    val p0 = keyframes.getOrNull(i - 2) ?: prev
                    val p3 = keyframes.getOrNull(i + 1) ?: curr
                    Pair(
                        SplineMath.catmullRomVec3d(p0.position, prev.position, curr.position, p3.position, easedT),
                        SplineMath.catmullRomVec2f(p0.rotation, prev.rotation, curr.rotation, p3.rotation, easedT),
                    )
                }
            }
        }

        val last = keyframes.last()
        return Pair(last.position, last.rotation)
    }
}
