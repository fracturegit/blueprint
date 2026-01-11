package net.mcbrawls.blueprint

import com.mojang.serialization.Codec

data class Vec2f(val x: Float, val y: Float) {
    operator fun plus(other: Vec2f) = Vec2f(x + other.x, y + other.y)
    operator fun minus(other: Vec2f) = Vec2f(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2f(x * scalar, y * scalar)

    companion object {
        val CODEC: Codec<Vec2f> = Codec.FLOAT.listOf(2, 2).xmap({ Vec2f(it[0], it[1]) }, { listOf(it.x, it.y) })
    }
}
