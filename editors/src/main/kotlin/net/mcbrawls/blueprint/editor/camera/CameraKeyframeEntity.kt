@file:Suppress("UnstableApiUsage")

package net.mcbrawls.blueprint.editor.camera

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.mcbrawls.blueprint.camera.CameraKeyframe
import net.mcbrawls.blueprint.camera.EasingFunction
import net.mcbrawls.blueprint.camera.PathMode
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

class CameraKeyframeEntity(
    val trackEntity: CameraTrackEntity,
    var duration: Double = 1.0,
    var pathMode: PathMode = PathMode.LINEAR,
    var easing: EasingFunction = EasingFunction.LINEAR,
) : Entity(EntityType.INTERACTION) {
    private val nametag = Nametag()

    val index: Int get() = trackEntity.keyframeEntities.indexOf(this)

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
            val isSelected = player.getTag(BlueprintEditorInstance.ACTIVE_KEYFRAME_TAG) == index &&
                    player.getTag(BlueprintEditorInstance.ACTIVE_TRACK_TAG) == trackEntity.trackId
            val color = if (isSelected) DyeColor.WHITE else DyeColor.MAGENTA
            player.sendPacket(
                ParticlePacket(
                    Particle.DUST.withProperties(color, 0.5f),
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

    fun updateNametag() { nametag.update() }

    fun toKeyframe(root: BlockVec): CameraKeyframe = CameraKeyframe(
        position = Vector3d(position.x - root.x(), position.y - root.y(), position.z - root.z()),
        rotation = Vector2f(position.yaw, position.pitch),
        duration = duration,
        pathMode = pathMode,
        easing = easing,
    )

    inner class Nametag : Entity(EntityType.TEXT_DISPLAY) {
        init {
            editEntityMeta(TextDisplayMeta::class.java) { meta ->
                buildText(meta)
                meta.billboardRenderConstraints = AbstractDisplayMeta.BillboardConstraints.CENTER
                meta.scale = Vec(0.4)
            }
            setNoGravity(true)
        }

        private fun buildText(meta: TextDisplayMeta) {
            meta.text = Component.text()
                .append(Component.text(trackEntity.trackId).decorate(TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("#${index + 1} | ${"%.1f".format(duration)}s | ${pathMode.name} | ${easing.name}"))
                .build()
        }

        fun update() {
            editEntityMeta(TextDisplayMeta::class.java) { meta -> buildText(meta) }
            this@CameraKeyframeEntity.sendPacketsToViewers(metadataPacket)
        }

        override fun updateNewViewer(viewer: Player) {
            position = this@CameraKeyframeEntity.position
            super.updateNewViewer(viewer)
            viewer.sendPacket(SetPassengersPacket(this@CameraKeyframeEntity.entityId, listOf(entityId)))
        }
    }
}
