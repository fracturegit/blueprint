@file:Suppress("UnstableApiUsage")

package net.mcbrawls.blueprint.editor.anchor

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.mcbrawls.blueprint.Marker
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

/**
 * Editor entity representing a whole [Marker] group.
 *
 * Positioned at the centroid of its anchor points (or the creation position if empty).
 * Right-clicking selects this marker as the active editing target for chat commands.
 * Individual anchor points are represented as [MarkerAnchorEntity] children.
 */
class MarkerGroupEntity(
    var markerName: String,
    var markerType: Key,
    var properties: Map<String, PropertyValue> = emptyMap(),
) : Entity(EntityType.INTERACTION) {
    /** The ordered list of anchor point entities belonging to this marker. */
    val anchorEntities: MutableList<MarkerAnchorEntity> = mutableListOf()

    private val nametag: Nametag = Nametag()

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
            val isSelected = player.getTag(BlueprintEditorInstance.ACTIVE_MARKER_TAG) == markerName
            val color = if (isSelected) DyeColor.ORANGE else DyeColor.LIME
            player.sendPacket(
                ParticlePacket(
                    Particle.DUST.withProperties(color, 0.8f),
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

    fun updateNametag() {
        nametag.updateNametag()
    }

    /**
     * Serialises this group and its anchor entities back to a [Marker], relative to [root].
     */
    fun createMarker(root: BlockVec): Marker {
        return Marker(
            type = markerType,
            anchors = anchorEntities.map { it.createAnchor(root) },
            properties = properties,
        )
    }

    inner class Nametag : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                updateName(meta)
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                meta.scale = Vec(0.55)
            }
            setNoGravity(true)
        }

        private fun updateName(meta: TextDisplayMeta) {
            val builder = Component.text()
                .append(Component.text("MARKER").decorate(TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text(markerName))
                .appendNewline()
                .append(Component.text(markerType.asString()))
                .appendNewline()
                .append(Component.text("${anchorEntities.size} anchor(s)"))

            // Show up to 3 properties inline
            var shown = 0
            for ((key, value) in properties) {
                if (shown >= 3) break
                builder.appendNewline().append(Component.text("$key: ${value.display}"))
                shown++
            }

            meta.text = builder.build()
        }

        override fun updateNewViewer(viewer: Player) {
            position = this@MarkerGroupEntity.position.add(0.0, 0.6, 0.0)
            super.updateNewViewer(viewer)
            viewer.sendPacket(SetPassengersPacket(this@MarkerGroupEntity.entityId, listOf(entityId)))
        }

        fun updateNametag() {
            editEntityMeta(TextDisplayMeta::class.java) { meta -> updateName(meta) }
            this@MarkerGroupEntity.sendPacketsToViewers(metadataPacket)
        }
    }
}
