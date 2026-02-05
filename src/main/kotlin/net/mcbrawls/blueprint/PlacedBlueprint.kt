package net.mcbrawls.blueprint

import net.mcbrawls.blueprint.box.BlockBox
import net.mcbrawls.blueprint.state.PalettedState
import org.joml.Vector3d
import org.joml.Vector3ic
import org.joml.plus

data class PlacedBlueprint<T>(
    val blueprint: Blueprint<T>,
    val position: Vector3ic,
) {
    val blockBox: BlockBox = BlockBox(position, position.plus(blueprint.size))

    fun getAllAnchors(): List<Pair<String, Anchor>> {
        return blueprint.anchors.map { (id, anchor) -> id to offsetAnchor(anchor) }
    }

    /**
     * Gets all anchors of a given id.
     */
    fun getAnchorsWithId(id: String): List<Anchor> {
        return blueprint.anchors
            .filter { (testedId, _) -> testedId == id }
            .map { (_, anchor) -> offsetAnchor(anchor) }
    }

    /**
     * Gets an anchor position from an anchor id assumed to be unique.
     * @return the placed offset position of the given anchor
     */
    fun getAnchorWithId(id: String): Anchor? {
        return getAnchorsWithId(id).firstOrNull()
    }

    /**
     * Gets a given anchor offset for this placed blueprint.
     */
    fun offsetAnchor(anchor: Anchor): Anchor {
        return Anchor(anchor.position + Vector3d(position), anchor.rotation, anchor.data)
    }

    fun forEachPosition(action: (Vector3ic) -> Unit) {
        blueprint.palettedStates
            .map(PalettedState::pos)
            .map { it + position }
            .forEach(action)
    }
}
