@file:Suppress("UnstableApiUsage")

package net.mcbrawls.fracture.blueprint.editor

import net.kyori.adventure.key.Key
import net.minestom.server.color.DyeColor
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.network.packet.server.play.SetPassengersPacket
import net.minestom.server.particle.Particle

class DecorationAnchorEntity(var modelKey: Key) : Entity(EntityType.INTERACTION) {
    private val modelEntity: Model = Model()

    init {
        setBoundingBox(1.0, 1.0, 1.0)

        editEntityMeta(InteractionMeta::class.java) { meta ->
            meta.width = boundingBox.width().toFloat()
            meta.height = boundingBox.height().toFloat()
            meta.response = true
        }

        setNoGravity(true)
    }

    override fun update(time: Long) {
        viewers.forEach { player ->
            val isModifying = player.getTag(BlueprintEditorInstance.ACTIVE_ANCHOR_TAG) == uuid
            val color = if (isModifying) DyeColor.ORANGE else DyeColor.RED
            player.sendPacket(ParticlePacket(Particle.DUST.withProperties(color, 0.5f), position.x, position.y + boundingBox.height() / 2, position.z, 0.0f, 0.0f, 0.0f, 0.0f, 1))
        }
    }

    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)
        modelEntity.updateNewViewer(player)
    }

    override fun updateOldViewer(player: Player) {
        super.updateOldViewer(player)
        modelEntity.updateOldViewer(player)
    }

    fun updateItem() {
        modelEntity.updateItem()
    }

    inner class Model : Entity(EntityType.ITEM_DISPLAY) {
        init {
            updateItem()
        }

        override fun updateNewViewer(viewer: Player) {
            val anchorEntity = this@DecorationAnchorEntity
            position = anchorEntity.position
            super.updateNewViewer(viewer)
            viewer.sendPacket(SetPassengersPacket(anchorEntity.entityId, listOf(entityId)))
        }

        fun updateItem() {
            editEntityMeta(ItemDisplayMeta::class.java) { meta ->
                meta.itemStack = ItemStack.builder(Material.CHORUS_FRUIT)
                    .set(DataComponents.ITEM_MODEL, modelKey.toString())
                    .build()
            }

            this@DecorationAnchorEntity.sendPacketsToViewers(metadataPacket)
        }
    }
}
