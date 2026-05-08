package net.mcbrawls.blueprint.camera

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object EasingFunctions {
    fun apply(fn: EasingFunction, t: Float): Float = when (fn) {
        EasingFunction.LINEAR -> t
        EasingFunction.STEP -> if (t >= 1f) 1f else 0f

        EasingFunction.SINE_IN -> 1f - cos(t * PI.toFloat() / 2f)
        EasingFunction.SINE_OUT -> sin(t * PI.toFloat() / 2f)
        EasingFunction.SINE_IN_OUT -> -(cos(PI.toFloat() * t) - 1f) / 2f

        EasingFunction.QUAD_IN -> t * t
        EasingFunction.QUAD_OUT -> 1f - (1f - t) * (1f - t)
        EasingFunction.QUAD_IN_OUT -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f

        EasingFunction.CUBIC_IN -> t * t * t
        EasingFunction.CUBIC_OUT -> 1f - (1f - t).pow(3)
        EasingFunction.CUBIC_IN_OUT -> if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f

        EasingFunction.QUART_IN -> t * t * t * t
        EasingFunction.QUART_OUT -> 1f - (1f - t).pow(4)
        EasingFunction.QUART_IN_OUT -> if (t < 0.5f) 8f * t * t * t * t else 1f - (-2f * t + 2f).pow(4) / 2f

        EasingFunction.QUINT_IN -> t * t * t * t * t
        EasingFunction.QUINT_OUT -> 1f - (1f - t).pow(5)
        EasingFunction.QUINT_IN_OUT -> if (t < 0.5f) 16f * t * t * t * t * t else 1f - (-2f * t + 2f).pow(5) / 2f

        EasingFunction.EXPO_IN -> if (t == 0f) 0f else 2f.pow(10f * t - 10f)
        EasingFunction.EXPO_OUT -> if (t == 1f) 1f else 1f - 2f.pow(-10f * t)
        EasingFunction.EXPO_IN_OUT -> when {
            t == 0f -> 0f; t == 1f -> 1f
            t < 0.5f -> 2f.pow(20f * t - 10f) / 2f
            else -> (2f - 2f.pow(-20f * t + 10f)) / 2f
        }

        EasingFunction.CIRC_IN -> 1f - sqrt(1f - t * t)
        EasingFunction.CIRC_OUT -> sqrt(1f - (t - 1f) * (t - 1f))
        EasingFunction.CIRC_IN_OUT ->
            if (t < 0.5f) (1f - sqrt(1f - (2f * t).pow(2))) / 2f
            else (sqrt(1f - (-2f * t + 2f).pow(2)) + 1f) / 2f

        EasingFunction.BACK_IN -> {
            val c3 = 2.70158f
            c3 * t * t * t - 1.70158f * t * t
        }
        EasingFunction.BACK_OUT -> {
            val c3 = 2.70158f
            1f + c3 * (t - 1f).pow(3) + 1.70158f * (t - 1f).pow(2)
        }
        EasingFunction.BACK_IN_OUT -> {
            val c2 = 2.5949095f
            if (t < 0.5f) ((2f * t).pow(2) * ((c2 + 1f) * 2f * t - c2)) / 2f
            else ((2f * t - 2f).pow(2) * ((c2 + 1f) * (t * 2f - 2f) + c2) + 2f) / 2f
        }

        EasingFunction.ELASTIC_IN -> {
            val c4 = (2f * PI / 3f).toFloat()
            when { t == 0f -> 0f; t == 1f -> 1f
                else -> -2f.pow(10f * t - 10f) * sin((t * 10f - 10.75f) * c4)
            }
        }
        EasingFunction.ELASTIC_OUT -> {
            val c4 = (2f * PI / 3f).toFloat()
            when { t == 0f -> 0f; t == 1f -> 1f
                else -> 2f.pow(-10f * t) * sin((t * 10f - 0.75f) * c4) + 1f
            }
        }
        EasingFunction.ELASTIC_IN_OUT -> {
            val c5 = (2f * PI / 4.5f).toFloat()
            when {
                t == 0f -> 0f; t == 1f -> 1f
                t < 0.5f -> -(2f.pow(20f * t - 10f) * sin((20f * t - 11.125f) * c5)) / 2f
                else -> (2f.pow(-20f * t + 10f) * sin((20f * t - 11.125f) * c5)) / 2f + 1f
            }
        }

        EasingFunction.BOUNCE_OUT -> bounceOut(t)
        EasingFunction.BOUNCE_IN -> 1f - bounceOut(1f - t)
        EasingFunction.BOUNCE_IN_OUT ->
            if (t < 0.5f) (1f - bounceOut(1f - 2f * t)) / 2f
            else (1f + bounceOut(2f * t - 1f)) / 2f
    }

    private fun bounceOut(t: Float): Float {
        val n1 = 7.5625f; val d1 = 2.75f
        return when {
            t < 1f / d1 -> n1 * t * t
            t < 2f / d1 -> { val t2 = t - 1.5f / d1; n1 * t2 * t2 + 0.75f }
            t < 2.5f / d1 -> { val t2 = t - 2.25f / d1; n1 * t2 * t2 + 0.9375f }
            else -> { val t2 = t - 2.625f / d1; n1 * t2 * t2 + 0.984375f }
        }
    }
}
