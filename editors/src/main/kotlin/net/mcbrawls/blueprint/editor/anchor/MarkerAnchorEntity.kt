@file:Suppress("UnstableApiUsage")

package net.mcbrawls.blueprint.editor.anchor

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.mcbrawls.blueprint.Anchor
import net.mcbrawls.blueprint.PropertyValue
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

/**
 * Editor entity representing a single anchor point within a [MarkerGroupEntity].
 *
 * Right-clicking selects this anchor as the active editing target.
 * Left-clicking (attack) removes it from its group.
 *
 * Colour: orange if selected, yellow if its parent marker is selected, red otherwise.
 */
class MarkerAnchorEntity(
    val markerGroup: MarkerGroupEntity,
    var properties: Map<String, PropertyValue> = emptyMap(),
) : Entity(EntityType.INTERACTION) {
    private val nametag: Nametag = Nametag()

    /** 1-based display index within the parent marker group. */
    val index: Int get() = markerGroup.anchorEntities.indexOf(this) + 1

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
            val isSelected = player.getTag(BlueprintEditorInstance.ACTIVE_ANCHOR_TAG) == uuid
            val isActiveMarker = player.getTag(BlueprintEditorInstance.ACTIVE_MARKER_TAG) == markerGroup.markerName
            val color = when {
                isSelected -> DyeColor.ORANGE
                isActiveMarker -> DyeColor.YELLOW
                else -> DyeColor.RED
            }
            player.sendPacket(
                ParticlePacket(
                    Particle.DUST.withProperties(color, 0.5f),
                    position.x, position.y + boundingBox.height() / 2, position.z,
                    0.0f, 0.0f, 0.0f, 0.0f, 1
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

    /**
     * Serialises this anchor entity back to an [Anchor], relative to [root].
     */
    fun updateNametag() {
        nametag.updateNametag()
    }

    fun createAnchor(root: BlockVec): Anchor {
        return Anchor(
            position = Vector3d(
                position.x - root.x(),
                position.y - root.y(),
                position.z - root.z(),
            ),
            rotation = Vector2f(position.yaw, position.pitch),
            properties = properties,
        )
    }

    inner class Nametag : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                updateName(meta)
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                meta.scale = Vec(0.4)
            }
            setNoGravity(true)
        }

        private fun updateName(meta: TextDisplayMeta) {
            val builder = Component.text()
                .append(Component.text(markerGroup.markerName).decorate(TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("#$index"))

            for ((key, value) in properties) {
                builder.appendNewline().append(Component.text("$key: ${value.display}"))
            }

            meta.text = builder.build()
        }

        override fun updateNewViewer(viewer: Player) {
            position = this@MarkerAnchorEntity.position
            super.updateNewViewer(viewer)
            viewer.sendPacket(SetPassengersPacket(this@MarkerAnchorEntity.entityId, listOf(entityId)))
        }

        fun updateNametag() {
            editEntityMeta(TextDisplayMeta::class.java) { meta -> updateName(meta) }
            this@MarkerAnchorEntity.sendPacketsToViewers(metadataPacket)
        }
    }
}
