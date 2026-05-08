@file:Suppress("UnstableApiUsage")

package net.mcbrawls.blueprint.editor.camera

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.color.DyeColor
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

class CameraTrackEntity(val trackId: String) : Entity(EntityType.INTERACTION) {
    val keyframeEntities: MutableList<CameraKeyframeEntity> = mutableListOf()
    private val nametag = Nametag()

    init {
        setBoundingBox(0.5, 0.5, 0.5)
        editEntityMeta(InteractionMeta::class.java) { meta ->
            meta.width = boundingBox.width().toFloat()
            meta.height = boundingBox.height().toFloat()
            meta.response = true
        }
        setNoGravity(true)
    }

    override fun update(time: Long) {
        viewers.forEach { player ->
            player.sendPacket(
                ParticlePacket(
                    Particle.DUST.withProperties(DyeColor.BLUE, 0.8f),
                    position.x, position.y + boundingBox.height() / 2, position.z,
                    0f, 0f, 0f, 0f, 1
                )
            )
        }
    }

    fun updateNametag() {
        val totalDuration = keyframeEntities.drop(1).sumOf { it.duration }
        nametag.update(keyframeEntities.size, totalDuration)
    }

    fun repositionFromKeyframes() {
        val first = keyframeEntities.firstOrNull() ?: return
        teleport(first.position.add(0.75, 0.0, 0.0))
    }

    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)
        nametag.updateNewViewer(player)
    }

    override fun updateOldViewer(player: Player) {
        super.updateOldViewer(player)
        nametag.updateOldViewer(player)
    }

    inner class Nametag : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                meta.text = Component.text(trackId).decorate(TextDecoration.BOLD)
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                meta.scale = Vec(0.55)
            }
            setNoGravity(true)
        }

        fun update(keyframeCount: Int, totalDuration: Double) {
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                meta.text = Component.text()
                    .append(Component.text("TRACK").decorate(TextDecoration.BOLD))
                    .appendNewline()
                    .append(Component.text(trackId))
                    .appendNewline()
                    .append(Component.text("$keyframeCount kf | ${"%.1f".format(totalDuration)}s"))
                    .build()
            }
            this@CameraTrackEntity.sendPacketsToViewers(metadataPacket)
        }

        override fun updateNewViewer(viewer: Player) {
            position = this@CameraTrackEntity.position.add(0.0, 0.6, 0.0)
            super.updateNewViewer(viewer)
            viewer.sendPacket(SetPassengersPacket(this@CameraTrackEntity.entityId, listOf(entityId)))
        }
    }
}
