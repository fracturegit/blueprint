package net.mcbrawls.blueprint

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.blueprint.box.VecBox
import net.mcbrawls.blueprint.camera.CameraTrack
import net.mcbrawls.blueprint.state.PalettedState
import net.mcbrawls.blueprint.state.State
import org.joml.Vector3i
import org.joml.Vector3ic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class Blueprint<T>(
    val palette: List<T>,
    val palettedStates: List<PalettedState>,
    val markers: Map<String, Marker>,
    val regions: Map<String, VecBox>,
    val waypoints: Map<String, Waypoint> = emptyMap(),
    val cameraTracks: Map<String, CameraTrack> = emptyMap(),
    val connectors: List<RoomConnector> = emptyList(),
) {
    val size: Vector3ic = calculateBlueprintSize(palettedStates.map(PalettedState::pos))

    fun forEach(action: (Vector3ic, T) -> Unit) {
        palettedStates.forEach { (offset, index) ->
            action.invoke(offset, palette[index])
        }
    }

    fun forEachPosition(action: (Vector3ic) -> Unit) {
        palettedStates.map(PalettedState::pos).forEach(action)
    }

    override fun toString(): String {
        return "Blueprint{${palette.size} unique, ${palettedStates.size} positions, ${markers.size} markers}"
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(Blueprint::class.java)

        fun <T> createCodec(to: (State) -> T, from: (T) -> State): Codec<Blueprint<T>> = RecordCodecBuilder.create { instance ->
            instance.group(
                State.CODEC.xmap(to, from).listOf().fieldOf("palette").forGetter(Blueprint<T>::palette),
                PalettedState.CODEC.listOf().fieldOf("block_states").forGetter(Blueprint<T>::palettedStates),
                Codec.unboundedMap(Codec.STRING, Marker.CODEC)
                    .optionalFieldOf("markers", emptyMap())
                    .forGetter(Blueprint<T>::markers),
                Codec.unboundedMap(Codec.STRING, VecBox.CODEC)
                    .optionalFieldOf("regions", emptyMap())
                    .forGetter(Blueprint<T>::regions),
                Codec.unboundedMap(Codec.STRING, Waypoint.CODEC)
                    .optionalFieldOf("waypoints", emptyMap())
                    .forGetter(Blueprint<T>::waypoints),
                Codec.unboundedMap(Codec.STRING, CameraTrack.CODEC)
                    .optionalFieldOf("camera_tracks", emptyMap())
                    .forGetter(Blueprint<T>::cameraTracks),
                RoomConnector.CODEC.listOf()
                    .optionalFieldOf("connectors", emptyList())
                    .forGetter(Blueprint<T>::connectors),
            ).apply(instance) { palette, states, markers, regions, waypoints, tracks, connectors ->
                Blueprint(palette, states, markers, regions, waypoints, tracks, connectors)
            }
        }

        fun calculateBlueprintSize(positions: List<Vector3ic>): Vector3i {
            if (positions.isEmpty()) return Vector3i()
            var minX = Int.MAX_VALUE; var minY = Int.MAX_VALUE; var minZ = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE; var maxY = Int.MIN_VALUE; var maxZ = Int.MIN_VALUE
            for (p in positions) {
                if (p.x() < minX) minX = p.x(); if (p.x() > maxX) maxX = p.x()
                if (p.y() < minY) minY = p.y(); if (p.y() > maxY) maxY = p.y()
                if (p.z() < minZ) minZ = p.z(); if (p.z() > maxZ) maxZ = p.z()
            }
            return Vector3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1)
        }
    }
}
