package net.mcbrawls.blueprint.editor.connector

import net.mcbrawls.blueprint.ConnectorType
import net.mcbrawls.blueprint.RoomConnector
import net.minestom.server.color.DyeColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle

/**
 * Editor entity that visualises a [RoomConnector] anchor.
 * Green particles = ENTRANCE, orange particles = EXIT.
 * Renders a directional indicator showing the connector's cardinal facing.
 */
class ConnectorMarkerEntity(val connector: RoomConnector) : Entity(EntityType.INTERACTION) {
    private val particleColor: DyeColor = when (connector.type) {
        ConnectorType.ENTRANCE -> DyeColor.LIME
        ConnectorType.EXIT -> DyeColor.ORANGE
    }

    init {
        setBoundingBox(0.3, 0.3, 0.3)
        setNoGravity(true)
    }

    override fun update(time: Long) {
        val step = connector.direction.step
        val px = position.x + step.x() * 0.5
        val py = position.y + 0.5
        val pz = position.z + step.z() * 0.5
        viewers.forEach { player ->
            player.sendPacket(
                ParticlePacket(
                    Particle.DUST.withProperties(particleColor, 0.6f),
                    px, py, pz,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 1,
                )
            )
        }
    }
}
