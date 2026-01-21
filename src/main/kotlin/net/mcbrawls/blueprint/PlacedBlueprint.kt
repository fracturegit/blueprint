package net.mcbrawls.blueprint

data class PlacedBlueprint<T>(
    val blueprint: Blueprint<T>,
    val position: Vec3i,
) {
    val box: Box = Box(position, position.plus(blueprint.size))

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
        return Anchor(anchor.position.add(position), anchor.rotation, anchor.data)
    }

    fun forEachPosition(action: (Vec3i) -> Unit) {
        blueprint.palettedStates
            .map(PalettedState::pos)
            .map { it + position }
            .forEach(action)
    }
}
