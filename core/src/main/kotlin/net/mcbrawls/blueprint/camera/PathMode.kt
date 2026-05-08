package net.mcbrawls.blueprint.camera

import com.mojang.serialization.Codec

enum class PathMode {
    LINEAR,
    CATMULL_ROM;

    companion object {
        val CODEC: Codec<PathMode> = Codec.STRING.xmap(
            { name -> valueOf(name.uppercase()) },
            { mode -> mode.name.lowercase() }
        )
    }
}
