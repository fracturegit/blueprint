@file:Suppress("UnstableApiUsage")

package net.mcbrawls.blueprint.editor.waypoint

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.mcbrawls.blueprint.Waypoint
import net.minestom.server.color.DyeColor
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.network.packet.server.play.SetPassengersPacket
import net.minestom.server.particle.Particle
import org.joml.Vector2f
import org.joml.Vector3d

class WaypointEntity(val waypointName: String) : Entity(EntityType.INTERACTION) {
    private val nametag = Nametag()

    init {
        setBoundingBox(0.3, 0.3, 0.3)
        editEntityMeta(InteractionMeta::class.java) { meta ->
            meta.width = boundingBox.width().toFloat()
            meta.height = boundingBox.height().toFloat()
            meta.response = true
        }
        setNoGravity(true)
        preventBlockPlacement = false
    }

    override fun update(time: Long) {
        viewers.forEach { player ->
            player.sendPacket(
                ParticlePacket(
                    Particle.DUST.withProperties(DyeColor.CYAN, 0.5f),
                    position.x, position.y + boundingBox.height() / 2, position.z,
                    0f, 0f, 0f, 0f, 1
                )
            )
        }
    }

    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)
        nametag.updateNewViewer(player)
    }

    override fun updateOldViewer(player: Player) {
        super.updateOldViewer(player)
        nametag.updateOldViewer(player)
    }

    fun teleportPlayer(player: Player) {
        player.teleport(position.sub(0.0, player.eyeHeight, 0.0).withView(position))
    }

    fun toWaypoint(root: BlockVec): Waypoint = Waypoint(
        position = Vector3d(position.x - root.x(), position.y - root.y(), position.z - root.z()),
        rotation = Vector2f(position.yaw, position.pitch),
    )

    inner class Nametag : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                meta.text = Component.text(waypointName).decorate(TextDecoration.BOLD)
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                meta.scale = Vec(0.4)
            }
            setNoGravity(true)
        }

        override fun updateNewViewer(viewer: Player) {
            position = this@WaypointEntity.position
            super.updateNewViewer(viewer)
            viewer.sendPacket(SetPassengersPacket(this@WaypointEntity.entityId, listOf(entityId)))
        }
    }
}
