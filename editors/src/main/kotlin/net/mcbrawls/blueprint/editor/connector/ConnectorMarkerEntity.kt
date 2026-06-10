package net.mcbrawls.blueprint.editor.connector

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.mcbrawls.blueprint.CardinalDirection
import net.mcbrawls.blueprint.ConnectorType
import net.mcbrawls.blueprint.RoomConnector
import net.minestom.server.color.DyeColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import org.joml.Vector3ic

/**
 * Editor entity that visualises a [RoomConnector] anchor.
 *
 * Green particles = ENTRANCE, orange particles = EXIT.
 * Renders a directional particle indicator each tick showing the connector's cardinal facing.
 *
 * [localPosition] is the block-local position within the blueprint (i.e. relative to the
 * blueprint origin, not the world position this entity is spawned at). This is what gets
 * written back into [RoomConnector] on save.
 */
class ConnectorMarkerEntity(
    val localPosition: Vector3ic,
    val direction: CardinalDirection,
    val type: ConnectorType,
) : Entity(EntityType.INTERACTION) {

    /** Reconstruct the [RoomConnector] this entity represents, using its local position. */
    val connector: RoomConnector get() = RoomConnector(localPosition, direction, type)

    private val particleColor: DyeColor = when (type) {
        ConnectorType.ENTRANCE -> DyeColor.LIME
        ConnectorType.EXIT -> DyeColor.ORANGE
    }

    init {
        val meta = entityMeta as InteractionMeta
        meta.width = 0.6f
        meta.height = 0.6f
        setNoGravity(true)
        preventBlockPlacement = false
    }

    fun updateNametag() {
        val typeLabel = type.name.lowercase()
        val dirLabel = direction.name.lowercase()
        val color = if (type == ConnectorType.ENTRANCE) NamedTextColor.GREEN else NamedTextColor.GOLD
        customName = Component.text("[$typeLabel] $dirLabel", color)
        isCustomNameVisible = true
    }

    override fun update(time: Long) {
        val step = direction.step
        // Particle floats slightly in front of the connector face to show direction
        val px = position.x + step.x() * 0.6
        val py = position.y + 0.5
        val pz = position.z + step.z() * 0.6
        viewers.forEach { player ->
            player.sendPacket(
                ParticlePacket(
                    Particle.DUST.withProperties(particleColor, 0.7f),
                    px, py, pz,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 1,
                )
            )
        }
    }

    companion object {
        /**
         * Create a [ConnectorMarkerEntity] from a [RoomConnector] that is already in world
         * coordinates — i.e. when loading existing connectors during editor initialisation.
         * [localPosition] is preserved separately since world coords are offset by the origin.
         */
        fun fromConnector(connector: RoomConnector): ConnectorMarkerEntity =
            ConnectorMarkerEntity(connector.position, connector.direction, connector.type)
    }
}
