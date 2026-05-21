package net.mcbrawls.blueprint.minestom

import net.mcbrawls.blueprint.Anchor
import net.mcbrawls.blueprint.Decoration
import net.minestom.server.coordinate.Pos

object MinestomBlueprints {
    val Anchor.combinedPos: Pos get() {
        return Pos(position.x(), position.y(), position.z(), rotation.x(), rotation.y())
    }

    val Decoration.combinedPos: Pos get() {
        return Pos(position.x(), position.y(), position.z(), rotation.x(), rotation.y())
    }
}
