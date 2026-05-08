package net.mcbrawls.blueprint.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EasingFunctionsTest {
    private val delta = 0.001f

    @Test fun `LINEAR returns t directly`() {
        assertEquals(0.0f, EasingFunctions.apply(EasingFunction.LINEAR, 0f), delta)
        assertEquals(0.5f, EasingFunctions.apply(EasingFunction.LINEAR, 0.5f), delta)
        assertEquals(1.0f, EasingFunctions.apply(EasingFunction.LINEAR, 1f), delta)
    }

    @Test fun `all non-STEP functions return 0 at t=0`() {
        EasingFunction.entries.filter { it != EasingFunction.STEP }.forEach { fn ->
            assertEquals(0f, EasingFunctions.apply(fn, 0f), delta, "Failed for $fn")
        }
    }

    @Test fun `all functions return 1 at t=1`() {
        EasingFunction.entries.forEach { fn ->
            assertEquals(1f, EasingFunctions.apply(fn, 1f), delta, "Failed for $fn")
        }
    }

    @Test fun `STEP returns 0 for t less than 1`() {
        assertEquals(0f, EasingFunctions.apply(EasingFunction.STEP, 0f), delta)
        assertEquals(0f, EasingFunctions.apply(EasingFunction.STEP, 0.5f), delta)
        assertEquals(0f, EasingFunctions.apply(EasingFunction.STEP, 0.999f), delta)
    }

    @Test fun `STEP returns 1 at t=1`() {
        assertEquals(1f, EasingFunctions.apply(EasingFunction.STEP, 1f), delta)
    }

    @Test fun `IN variants are slower than LINEAR at t=0_5`() {
        val linearMid = 0.5f
        listOf(
            EasingFunction.SINE_IN, EasingFunction.QUAD_IN, EasingFunction.CUBIC_IN,
            EasingFunction.QUART_IN, EasingFunction.QUINT_IN, EasingFunction.EXPO_IN,
            EasingFunction.CIRC_IN,
        ).forEach { fn ->
            assertTrue(EasingFunctions.apply(fn, 0.5f) < linearMid, "$fn should be below linear at t=0.5")
        }
    }

    @Test fun `OUT variants are faster than LINEAR at t=0_5`() {
        val linearMid = 0.5f
        listOf(
            EasingFunction.SINE_OUT, EasingFunction.QUAD_OUT, EasingFunction.CUBIC_OUT,
            EasingFunction.QUART_OUT, EasingFunction.QUINT_OUT, EasingFunction.EXPO_OUT,
            EasingFunction.CIRC_OUT,
        ).forEach { fn ->
            assertTrue(EasingFunctions.apply(fn, 0.5f) > linearMid, "$fn should be above linear at t=0.5")
        }
    }

    @Test fun `all functions are monotonically non-decreasing for standard easings`() {
        val standardFunctions = EasingFunction.entries.filter { fn ->
            fn !in setOf(EasingFunction.BACK_IN, EasingFunction.BACK_OUT, EasingFunction.BACK_IN_OUT,
                EasingFunction.ELASTIC_IN, EasingFunction.ELASTIC_OUT, EasingFunction.ELASTIC_IN_OUT,
                EasingFunction.BOUNCE_IN, EasingFunction.BOUNCE_OUT, EasingFunction.BOUNCE_IN_OUT,
                EasingFunction.STEP)
        }
        val steps = (0..20).map { it / 20f }
        standardFunctions.forEach { fn ->
            val values = steps.map { EasingFunctions.apply(fn, it) }
            values.zipWithNext().forEachIndexed { i, (a, b) ->
                assertTrue(b >= a - delta, "$fn dropped from $a to $b at step $i")
            }
        }
    }
}
