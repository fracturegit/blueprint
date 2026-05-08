package net.mcbrawls.blueprint.camera

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class CameraTrack(
    val keyframes: List<CameraKeyframe>,
) {
    companion object {
        val CODEC: Codec<CameraTrack> = RecordCodecBuilder.create { instance ->
            instance.group(
                CameraKeyframe.CODEC.listOf().fieldOf("keyframes").forGetter(CameraTrack::keyframes),
            ).apply(instance, ::CameraTrack)
        }
    }
}
