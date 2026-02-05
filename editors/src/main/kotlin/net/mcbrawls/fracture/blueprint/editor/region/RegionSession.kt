package net.mcbrawls.fracture.blueprint.editor.region

import org.joml.Vector3dc
import java.util.UUID

/**
 * Represents a session where a player is defining a new region.
 */
data class RegionSession(
    val playerId: UUID,
    var pos1: Vector3dc? = null,
    var pos2: Vector3dc? = null,
)
