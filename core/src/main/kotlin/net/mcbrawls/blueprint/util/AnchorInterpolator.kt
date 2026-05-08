package net.mcbrawls.blueprint.util

import net.mcbrawls.blueprint.Anchor

class AnchorInterpolator(private val anchors: List<Anchor>) {
    init {
        require(anchors.size >= 2) { "At least 2 anchors are required for interpolation" }
    }

    fun interpolate(progress: Float): Anchor {
        val clampedProgress = progress.coerceIn(0f, 1f)
        if (anchors.size == 2) return interpolateLinear(clampedProgress)
        if (clampedProgress == 0f) return anchors.first()
        if (clampedProgress == 1f) return anchors.last()

        val segmentCount = anchors.size - 1
        val segmentProgress = clampedProgress * segmentCount
        val segmentIndex = segmentProgress.toInt().coerceIn(0, segmentCount - 1)
        val t = segmentProgress - segmentIndex

        val p0 = anchors.getOrNull(segmentIndex - 1) ?: anchors[segmentIndex]
        val p1 = anchors[segmentIndex]
        val p2 = anchors[segmentIndex + 1]
        val p3 = anchors.getOrNull(segmentIndex + 2) ?: anchors[segmentIndex + 1]

        return Anchor(
            SplineMath.catmullRomVec3d(p0.position, p1.position, p2.position, p3.position, t),
            SplineMath.catmullRomVec2f(p0.rotation, p1.rotation, p2.rotation, p3.rotation, t),
        )
    }

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

        return Anchor(
            SplineMath.lerpVec3d(startAnchor.position, endAnchor.position, t),
            SplineMath.lerpVec2f(startAnchor.rotation, endAnchor.rotation, t),
        )
    }
}
