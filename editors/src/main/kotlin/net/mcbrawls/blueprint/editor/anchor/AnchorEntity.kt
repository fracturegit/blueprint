@file:Suppress("UnstableApiUsage")

package net.mcbrawls.blueprint.editor.anchor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.mcbrawls.blueprint.Anchor
import net.mcbrawls.blueprint.editor.BlueprintEditorInstance
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
import java.util.Optional

class AnchorEntity(var anchorId: String, anchor: Anchor) : Entity(EntityType.INTERACTION) {
    var anchorData: String? = anchor.data.orElse(null)
    private val nametag: Nametag = Nametag()

    init {
        setBoundingBox(0.3, 0.3, 0.3)

        editEntityMeta(InteractionMeta::class.java) { meta ->
            meta.width = boundingBox.width().toFloat()
            meta.height = boundingBox.height().toFloat()
            meta.response = true
        }

        setNoGravity(true)
    }

    override fun update(time: Long) {
        viewers.forEach { player ->
            val isModifying = player.getTag(BlueprintEditorInstance.Companion.ACTIVE_ANCHOR_TAG) == uuid
            val color = if (isModifying) DyeColor.ORANGE else DyeColor.RED
            player.sendPacket(ParticlePacket(Particle.DUST.withProperties(color, 0.5f), position.x, position.y + boundingBox.height() / 2, position.z, 0.0f, 0.0f, 0.0f, 0.0f, 1))
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

    fun updateNametag() {
        nametag.updateNametag()
    }

    fun createAnchor(root: BlockVec): Anchor {
        val pos = Vector3d(position.x - root.x(), position.y - root.y(), position.z - root.z())
        return Anchor(pos, Vector2f(position.yaw, position.pitch), Optional.ofNullable(anchorData))
    }

    inner class Nametag : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                updateName(meta)
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                meta.scale = Vec(0.5)
            }

            setNoGravity(true)
        }

        private fun updateName(meta: TextDisplayMeta) {
            val component = Component.text()

            component.append(Component.text("ANCHOR").decorate(TextDecoration.BOLD))
            component.appendNewline()

            component.append(Component.text("Id: $anchorId"))

            anchorData?.let { data ->
                component.appendNewline()
                component.append(Component.text("Data: $data"))
            }

            meta.text = component.build()
        }

        override fun updateNewViewer(viewer: Player) {
            val anchorEntity = this@AnchorEntity
            position = anchorEntity.position
            super.updateNewViewer(viewer)
            viewer.sendPacket(SetPassengersPacket(anchorEntity.entityId, listOf(entityId)))
        }

        fun updateNametag() {
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                updateName(meta)
            }

            this@AnchorEntity.sendPacketsToViewers(metadataPacket)
        }
    }
}
