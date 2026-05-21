@file:Suppress("UnstableApiUsage")

package net.mcbrawls.blueprint.editor.decoration

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.mcbrawls.blueprint.Decoration
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
 * Editor entity representing a single [Decoration] point.
 *
 * Right-clicking selects this decoration as the active editing target.
 * Left-clicking removes it.
 * An optional visual [previewEntity] is attached as a passenger and removed together.
 */
class DecorationEntity(
    var decorationType: Key,
    var properties: Map<String, PropertyValue> = emptyMap(),
    var previewEntity: Entity? = null,
) : Entity(EntityType.INTERACTION) {

    private val nametag: Nametag = Nametag()

    init {
        setBoundingBox(0.6, 0.6, 0.6)
        editEntityMeta(InteractionMeta::class.java) { meta ->
            meta.width = boundingBox.width().toFloat()
            meta.height = boundingBox.height().toFloat()
            meta.response = true
        }
        setNoGravity(true)
    }

    override fun update(time: Long) {
        viewers.forEach { player ->
            val isSelected = player.getTag(BlueprintEditorInstance.ACTIVE_DECORATION_TAG) == entityId
            val color = if (isSelected) DyeColor.CYAN else DyeColor.BLUE
            player.sendPacket(
                ParticlePacket(
                    Particle.DUST.withProperties(color, 0.6f),
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

    fun createDecoration(root: BlockVec): Decoration {
        return Decoration(
            type = decorationType,
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
                meta.scale = Vec(0.5)
            }
            setNoGravity(true)
        }

        private fun updateName(meta: TextDisplayMeta) {
            val builder = Component.text()
                .append(Component.text("DECORATION", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text(decorationType.asString()))

            var shown = 0
            for ((key, value) in properties) {
                if (shown >= 3) break
                builder.appendNewline().append(Component.text("$key: ${value.display}"))
                shown++
            }

            meta.text = builder.build()
        }

        override fun updateNewViewer(viewer: Player) {
            position = this@DecorationEntity.position.add(0.0, 0.7, 0.0)
            super.updateNewViewer(viewer)
            viewer.sendPacket(SetPassengersPacket(this@DecorationEntity.entityId, listOf(entityId)))
        }

        fun updateNametag() {
            editEntityMeta(TextDisplayMeta::class.java) { meta -> updateName(meta) }
            this@DecorationEntity.sendPacketsToViewers(metadataPacket)
        }
    }
}
