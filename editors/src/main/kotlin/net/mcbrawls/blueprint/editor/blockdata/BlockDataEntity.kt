@file:Suppress("UnstableApiUsage")

package net.mcbrawls.blueprint.editor.blockdata

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.mcbrawls.blueprint.PropertyValue
import net.mcbrawls.blueprint.editor.BlueprintEditorInstance
import net.minestom.server.color.DyeColor
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.network.packet.server.play.SetPassengersPacket
import net.minestom.server.particle.Particle
import org.joml.Vector3i

/**
 * Editor entity that visualises a block with custom data at a specific world position.
 *
 * Purely visual — no hitbox. Selection is done via feather right-click on the block itself.
 */
class BlockDataEntity(
    val worldPos: Vector3i,
    localPos: Vector3i,
    var properties: Map<String, PropertyValue> = emptyMap(),
) : Entity(EntityType.MARKER) {

    private val localPosLabel = "(${localPos.x},${localPos.y},${localPos.z})"
    private val activeBlockTag = "${worldPos.x},${worldPos.y},${worldPos.z}"
    private val nametag = Nametag()

    init {
        setNoGravity(true)
    }

    override fun update(time: Long) {
        viewers.forEach { player ->
            val isSelected = player.getTag(BlueprintEditorInstance.ACTIVE_BLOCK_TAG) == activeBlockTag
            val color = if (isSelected) DyeColor.ORANGE else DyeColor.CYAN
            player.sendPacket(
                ParticlePacket(
                    Particle.DUST.withProperties(color, 0.8f),
                    position.x, position.y + 0.5, position.z,
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

    inner class Nametag : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                updateName(meta)
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                meta.scale = Vec(0.5)
                meta.isSeeThrough = true
            }
            setNoGravity(true)
        }

        private fun updateName(meta: TextDisplayMeta) {
            val builder = Component.text()
                .append(Component.text("BLOCK DATA").decorate(TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text(localPosLabel))

            for ((key, value) in properties) {
                builder.appendNewline().append(Component.text("$key: ${value.display}"))
            }

            meta.text = builder.build()
        }

        override fun updateNewViewer(viewer: Player) {
            position = this@BlockDataEntity.position.add(0.0, 0.5, 0.0)
            super.updateNewViewer(viewer)
            viewer.sendPacket(SetPassengersPacket(this@BlockDataEntity.entityId, listOf(entityId)))
        }

        fun updateNametag() {
            editEntityMeta(TextDisplayMeta::class.java) { meta -> updateName(meta) }
            this@BlockDataEntity.sendPacketsToViewers(metadataPacket)
        }
    }
}
