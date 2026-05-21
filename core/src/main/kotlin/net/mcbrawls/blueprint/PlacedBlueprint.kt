package net.mcbrawls.blueprint

import net.kyori.adventure.key.Key
import net.mcbrawls.blueprint.box.BlockBox
import net.mcbrawls.blueprint.box.VecBox
import net.mcbrawls.blueprint.camera.CameraTrack
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic

data class PlacedBlueprint<T>(
    val blueprint: Blueprint<T>,
    val position: Vector3ic,
    val rotation: Rotation = Rotation.NONE,
) {
    /**
     * The bounding box of this placed blueprint in world coordinates.
     * Uses the rotation-aware size so rotated rooms (CW_90/CW_270) have correct x/z extents.
     */
    val blockBox: BlockBox = run {
        val size = rotation.rotatedSize(blueprint.size)
        BlockBox(
            Vector3i(position),
            Vector3i(
                position.x() + size.x() - 1,
                position.y() + size.y() - 1,
                position.z() + size.z() - 1,
            ),
        )
    }

    /**
     * Internal helpers
     */

    /** Rotates a continuous local position to world space (rotate then translate). */
    private fun toWorld(local: Vector3dc): Vector3d =
        rotation.rotateVec3d(local, blueprint.size).add(Vector3d(position))

    /** Rotates an integer local position to world space (rotate then translate). */
    private fun toWorldInt(local: Vector3ic): Vector3ic =
        rotation.rotateVec3i(local, blueprint.size).add(position.x(), position.y(), position.z())

    /**
     * Regions
     */

    /**
     * Returns the named region rotated and offset to world coordinates, or null if absent.
     */
    fun getRegion(id: String): VecBox? {
        return blueprint.regions[id]
            ?.rotated(rotation, blueprint.size)
            ?.offset(Vector3d(position))
    }

    /**
     * Markers / Anchors
     */

    /**
     * Gets a marker by name with all anchor positions rotated and offset to world coordinates.
     */
    fun getMarker(name: String): Marker? {
        return blueprint.markers[name]?.let { offsetMarker(it) }
    }

    /**
     * Gets the first anchor of the named marker in world coordinates, or null if the marker
     * doesn't exist or has no anchors. Shorthand for single-anchor markers.
     */
    fun getFirstAnchor(name: String): Anchor? {
        return getMarker(name)?.anchors?.firstOrNull()
    }

    /**
     * Gets all anchors of the named marker in world coordinates, or an empty list if absent.
     */
    fun getAnchors(name: String): List<Anchor> {
        return getMarker(name)?.anchors.orEmpty()
    }

    /**
     * Gets all markers of a given type with anchors rotated and offset to world coordinates.
     */
    fun getMarkersOfType(type: Key): Map<String, Marker> {
        return blueprint.markers
            .filterValues { it.type == type }
            .mapValues { (_, marker) -> offsetMarker(marker) }
    }

    /**
     * Gets all markers with anchors rotated and offset to world coordinates.
     */
    fun getAllMarkers(): Map<String, Marker> {
        return blueprint.markers.mapValues { (_, marker) -> offsetMarker(marker) }
    }

    private fun offsetMarker(marker: Marker): Marker {
        return marker.copy(anchors = marker.anchors.map { offsetAnchor(it) })
    }

    /**
     * Returns [anchor] with its position rotated and offset to world coordinates,
     * and its yaw rotated by this placement's rotation.
     */
    fun offsetAnchor(anchor: Anchor): Anchor {
        // Rotate only the yaw (x); pitch (y) is unaffected by a Y-axis blueprint rotation.
        val rotatedRotation = Vector2f(rotation.rotateYaw(anchor.rotation.x()), anchor.rotation.y())
        return Anchor(
            toWorld(anchor.position),
            rotatedRotation,
            anchor.properties,
        )
    }

    /**
     * Waypoints
     */

    fun getWaypoint(name: String): Waypoint? {
        return blueprint.waypoints[name]?.let { offsetWaypoint(it) }
    }

    fun getAllWaypoints(): Map<String, Waypoint> {
        return blueprint.waypoints.mapValues { (_, waypoint) -> offsetWaypoint(waypoint) }
    }

    private fun offsetWaypoint(waypoint: Waypoint): Waypoint {
        return waypoint.copy(position = toWorld(waypoint.position))
    }

    /**
     * Camera tracks
     */

    fun getCameraTrack(id: String): CameraTrack? {
        return blueprint.cameraTracks[id]?.let { offsetCameraTrack(it) }
    }

    fun getAllCameraTracks(): Map<String, CameraTrack> {
        return blueprint.cameraTracks.mapValues { (_, track) -> offsetCameraTrack(track) }
    }

    private fun offsetCameraTrack(track: CameraTrack): CameraTrack {
        return track.copy(keyframes = track.keyframes.map { kf ->
            kf.copy(position = toWorld(kf.position))
            // If CameraKeyframe also carries a yaw/pitch, rotate yaw here too:
            // kf.copy(position = toWorld(kf.position), yaw = rotation.rotateYaw(kf.yaw))
        })
    }

    /**
     * Decorations
     */

    fun getDecorations(): List<Decoration> {
        return blueprint.decorations.map { offsetDecoration(it) }
    }

    private fun offsetDecoration(decoration: Decoration): Decoration {
        val rotatedRotation = Vector2f(rotation.rotateYaw(decoration.rotation.x()), decoration.rotation.y())
        return Decoration(
            type = decoration.type,
            position = toWorld(decoration.position),
            rotation = rotatedRotation,
            properties = decoration.properties,
        )
    }

    /**
     * Block positions
     */

    fun forEachPosition(action: (Vector3ic) -> Unit) {
        blueprint.palettedStates.forEach { action(toWorldInt(it.pos)) }
    }
}
