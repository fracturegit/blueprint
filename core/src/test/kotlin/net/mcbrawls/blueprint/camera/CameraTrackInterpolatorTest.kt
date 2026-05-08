package net.mcbrawls.blueprint.camera

import org.joml.Vector2f
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CameraTrackInterpolatorTest {
    private val delta = 0.001

    private fun kf(x: Double, duration: Double = 1.0, easing: EasingFunction = EasingFunction.LINEAR) =
        CameraKeyframe(Vector3d(x, 0.0, 0.0), Vector2f(0f, 0f), duration, PathMode.LINEAR, easing)

    @Test fun `totalDuration sums keyframes from index 1 onwards`() {
        val track = CameraTrack(listOf(kf(0.0, 0.0), kf(10.0, 2.0), kf(20.0, 3.0)))
        assertEquals(5.0, CameraTrackInterpolator(track).totalDuration, delta)
    }

    @Test fun `totalDuration is 0 for single keyframe`() {
        val track = CameraTrack(listOf(kf(5.0, 0.0)))
        assertEquals(0.0, CameraTrackInterpolator(track).totalDuration, delta)
    }

    @Test fun `interpolate at 0 returns first keyframe position`() {
        val track = CameraTrack(listOf(kf(0.0, 0.0), kf(10.0, 1.0)))
        val (pos, _) = CameraTrackInterpolator(track).interpolate(0.0)
        assertEquals(0.0, pos.x(), delta)
    }

    @Test fun `interpolate at totalDuration returns last keyframe position`() {
        val track = CameraTrack(listOf(kf(0.0, 0.0), kf(10.0, 1.0)))
        val (pos, _) = CameraTrackInterpolator(track).interpolate(1.0)
        assertEquals(10.0, pos.x(), delta)
    }

    @Test fun `LINEAR interpolation at segment midpoint returns midpoint position`() {
        val track = CameraTrack(listOf(kf(0.0, 0.0), kf(10.0, 2.0)))
        val (pos, _) = CameraTrackInterpolator(track).interpolate(1.0)
        assertEquals(5.0, pos.x(), delta)
    }

    @Test fun `single keyframe returns that keyframe for any time`() {
        val track = CameraTrack(listOf(kf(7.0, 0.0)))
        val (pos, _) = CameraTrackInterpolator(track).interpolate(0.0)
        assertEquals(7.0, pos.x(), delta)
    }

    @Test fun `empty track returns zero position`() {
        val track = CameraTrack(emptyList())
        val (pos, _) = CameraTrackInterpolator(track).interpolate(0.0)
        assertEquals(0.0, pos.x(), delta)
    }

    @Test fun `STEP easing stays at previous keyframe position mid-segment`() {
        val track = CameraTrack(listOf(kf(0.0, 0.0), kf(10.0, 2.0, EasingFunction.STEP)))
        val (pos, _) = CameraTrackInterpolator(track).interpolate(1.0)
        assertEquals(0.0, pos.x(), delta)
    }

    @Test fun `STEP easing returns target keyframe position at segment end`() {
        val track = CameraTrack(listOf(kf(0.0, 0.0), kf(10.0, 2.0, EasingFunction.STEP)))
        val (pos, _) = CameraTrackInterpolator(track).interpolate(2.0)
        assertEquals(10.0, pos.x(), delta)
    }

    @Test fun `multi-segment track selects correct segment`() {
        val track = CameraTrack(listOf(kf(0.0, 0.0), kf(10.0, 1.0), kf(30.0, 1.0)))
        // At t=1.5 we are in segment 2 (10→30), 50% through → position 20
        val (pos, _) = CameraTrackInterpolator(track).interpolate(1.5)
        assertEquals(20.0, pos.x(), delta)
    }

    @Test fun `time clamped above totalDuration returns last keyframe`() {
        val track = CameraTrack(listOf(kf(0.0, 0.0), kf(10.0, 1.0)))
        val (pos, _) = CameraTrackInterpolator(track).interpolate(99.0)
        assertEquals(10.0, pos.x(), delta)
    }
}
