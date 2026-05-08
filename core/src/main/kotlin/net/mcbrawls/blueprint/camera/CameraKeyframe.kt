package net.mcbrawls.blueprint.camera

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector2fc
import org.joml.Vector3dc

data class CameraKeyframe(
    val position: Vector3dc,
    val rotation: Vector2fc,
    val duration: Double,
    val pathMode: PathMode,
    val easing: EasingFunction,
) {
    companion object {
        val CODEC: Codec<CameraKeyframe> = RecordCodecBuilder.create { instance ->
            instance.group(
                VectorCodecs.VECTOR_3D.fieldOf("position").forGetter(CameraKeyframe::position),
                VectorCodecs.VECTOR_2F.fieldOf("rotation").forGetter(CameraKeyframe::rotation),
                Codec.DOUBLE.fieldOf("duration").forGetter(CameraKeyframe::duration),
                PathMode.CODEC.fieldOf("path_mode").forGetter(CameraKeyframe::pathMode),
                EasingFunction.CODEC.fieldOf("easing").forGetter(CameraKeyframe::easing),
            ).apply(instance, ::CameraKeyframe)
        }
    }
}
