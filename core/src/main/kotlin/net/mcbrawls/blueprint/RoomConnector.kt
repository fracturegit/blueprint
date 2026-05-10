package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.serialization.VectorCodecs
import org.joml.Vector3i
import org.joml.Vector3ic

data class RoomConnector(
    val position: Vector3ic,
    val direction: CardinalDirection,
    val type: ConnectorType,
) {
    companion object {
        val CODEC: Codec<RoomConnector> = RecordCodecBuilder.create { instance ->
            instance.group(
                VectorCodecs.VECTOR_3I.fieldOf("position").forGetter { rc -> Vector3i(rc.position) },
                Codec.STRING
                    .comapFlatMap(
                        { s -> runCatching { CardinalDirection.valueOf(s.uppercase()) }
                            .fold({ DataResult.success(it) }, { DataResult.error { "Unknown direction: $s" } }) },
                        { d -> d.name.lowercase() }
                    )
                    .fieldOf("direction")
                    .forGetter(RoomConnector::direction),
                Codec.STRING
                    .comapFlatMap(
                        { s -> runCatching { ConnectorType.valueOf(s.uppercase()) }
                            .fold({ DataResult.success(it) }, { DataResult.error { "Unknown connector type: $s" } }) },
                        { t -> t.name.lowercase() }
                    )
                    .fieldOf("type")
                    .forGetter(RoomConnector::type),
            ).apply(instance, ::RoomConnector)
        }
    }
}

fun RoomConnector.rotate(rotation: Rotation, blueprintSize: Vector3ic): RoomConnector {
    val x = position.x(); val y = position.y(); val z = position.z()
    val sx = blueprintSize.x(); val sz = blueprintSize.z()
    val newPos: Vector3ic = when (rotation) {
        Rotation.NONE -> Vector3i(x, y, z)
        Rotation.CW_90 -> Vector3i(sz - 1 - z, y, x)
        Rotation.CW_180 -> Vector3i(sx - 1 - x, y, sz - 1 - z)
        Rotation.CW_270 -> Vector3i(z, y, sx - 1 - x)
    }
    return RoomConnector(newPos, rotation.rotate(direction), type)
}
