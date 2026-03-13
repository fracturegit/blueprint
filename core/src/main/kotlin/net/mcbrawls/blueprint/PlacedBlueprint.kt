package net.mcbrawls.blueprint

import net.kyori.adventure.key.Key
import net.mcbrawls.blueprint.box.BlockBox
import net.mcbrawls.blueprint.box.VecBox
import org.joml.Vector3d
import org.joml.Vector3ic
import org.joml.plus

data class PlacedBlueprint<T>(
    val blueprint: Blueprint<T>,
    val position: Vector3ic,
) {
    val blockBox: BlockBox = BlockBox(position, position.plus(blueprint.size))

    fun getRegion(id: String): VecBox? {
        return blueprint.regions[id]?.offset(Vector3d(position))
    }

    /**
     * Gets a marker by name with all anchor positions offset to world coordinates.
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
     * Gets all markers of a given type with anchors in world coordinates.
     */
    fun getMarkersOfType(type: Key): Map<String, Marker> {
        return blueprint.markers
            .filterValues { it.type == type }
            .mapValues { (_, marker) -> offsetMarker(marker) }
    }

    /**
     * Gets all markers with anchors in world coordinates.
     */
    fun getAllMarkers(): Map<String, Marker> {
        return blueprint.markers.mapValues { (_, marker) -> offsetMarker(marker) }
    }

    private fun offsetMarker(marker: Marker): Marker {
        return marker.copy(anchors = marker.anchors.map { offsetAnchor(it) })
    }

    fun offsetAnchor(anchor: Anchor): Anchor {
        return Anchor(anchor.position + Vector3d(position), anchor.rotation, anchor.properties)
    }

    fun forEachPosition(action: (Vector3ic) -> Unit) {
        blueprint.palettedStates.forEach { action(it.pos + position) }
    }
}
