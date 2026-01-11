package net.mcbrawls.blueprint

data class PlacedBlueprint<T>(
    val blueprint: Blueprint<T>,
    val position: Vec3i,
) {
    val box: Box = Box(position, position.plus(blueprint.size))

    /**
     * Gets all anchors of a given id.
     */
    fun getAnchors(id: String): List<Anchor> {
        return blueprint.anchors
            .filter { (testedId, _) -> testedId == id }
            .map { (_, anchor) -> getAnchorOffset(anchor) }
    }

    /**
     * Gets a given anchor offset for this placed blueprint.
     */
    fun getAnchorOffset(anchor: Anchor): Anchor {
        return Anchor(anchor.position.add(position), anchor.rotation, anchor.data)
    }

    /**
     * Gets an anchor position from an anchor id assumed to be unique.
     * @return the placed offset position of the given anchor
     */
    fun getUniqueAnchor(id: String): Anchor? {
        return getAnchors(id).firstOrNull()
    }

    fun forEachPosition(action: (Vec3i) -> Unit) {
        blueprint.palettedStates
            .map(PalettedState::pos)
            .map { it + position }
            .forEach(action)
    }
}
