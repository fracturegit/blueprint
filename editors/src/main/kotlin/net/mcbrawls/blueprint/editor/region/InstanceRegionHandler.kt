package net.mcbrawls.blueprint.editor.region

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.mcbrawls.blueprint.box.VecBox
import net.mcbrawls.blueprint.editor.BlueprintEditorInstance
import net.minestom.server.color.Color
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
import org.joml.Vector3d
import org.joml.Vector3dc
import java.util.UUID

/**
 * Manages the creation and visualization of regions in the blueprint editor.
 */
class InstanceRegionHandler(private val instance: BlueprintEditorInstance, root: BlockVec, regions: Map<String, VecBox>) {
    private val creationSessions: MutableMap<UUID, RegionSession> = mutableMapOf()
    private val playerParticleSettings: MutableMap<UUID, Boolean> = mutableMapOf()
    private var particleUpdateTick: Long = 0

    private val regions: MutableMap<String, VecBox> = regions
        .mapValues { (_, box) -> box.offset(Vector3d(root.x(), root.y(), root.z())) }
        .toMutableMap()

    private val regionDisplays: MutableMap<String, RegionEntity> = mutableMapOf()

    fun initialize() {
        regions.forEach(::addRegionDisplay)
    }

    /**
     * Toggle particle visualization for a specific player.
     * @return true if particles are now enabled for this player
     */
    fun togglePlayerParticleVisualization(player: Player): Boolean {
        val currentState = playerParticleSettings[player.uuid] ?: true
        val newState = !currentState
        playerParticleSettings[player.uuid] = newState
        return newState
    }

    /**
     * Check if particle visualization is enabled for a specific player.
     */
    fun isParticleVisualizationEnabled(player: Player): Boolean {
        return playerParticleSettings[player.uuid] ?: true  // Default to true
    }

    /**
     * Clean up player settings when they leave.
     */
    fun removePlayer(player: Player) {
        playerParticleSettings.remove(player.uuid)
        creationSessions.remove(player.uuid)
    }

    fun enterCreationMode(player: Player) {
        if (creationSessions.containsKey(player.uuid)) {
            player.sendActionBar(Component.text("Already in region creation mode", NamedTextColor.YELLOW))
            return
        }

        creationSessions[player.uuid] = RegionSession(player.uuid)
        player.sendActionBar(Component.text("Entered region creation mode. Click to set position 1, then position 2", NamedTextColor.GREEN))
    }

    fun exitCreationMode(player: Player) {
        creationSessions.remove(player.uuid)
        player.sendActionBar(Component.text("Exited region creation mode", NamedTextColor.GRAY))
    }

    fun setPosition(player: Player, position: Vector3d): RegionSession? {
        val session = creationSessions[player.uuid] ?: return null

        return when {
            session.pos1 == null -> {
                session.pos1 = position
                player.sendActionBar(Component.text("Position 1 set. Click to set position 2", NamedTextColor.GREEN))
                session
            }
            session.pos2 == null -> {
                session.pos2 = position
                player.sendActionBar(Component.text("Position 2 set. Type region ID in chat", NamedTextColor.GREEN))
                session
            }
            else -> {
                player.sendActionBar(Component.text("Both positions already set. Type region ID in chat", NamedTextColor.YELLOW))
                session
            }
        }
    }

    fun confirmRegion(player: Player, regionId: String): Boolean {
        val session = creationSessions[player.uuid] ?: return false

        val pos1 = session.pos1 ?: return false
        val pos2 = session.pos2 ?: return false

        val box = VecBox(pos1, pos2)
        regions[regionId] = box

        // Create and spawn region display
        addRegionDisplay(regionId, box)

        creationSessions.remove(player.uuid)
        player.sendActionBar(Component.text("Region '$regionId' created", NamedTextColor.GREEN))

        return true
    }

    private fun addRegionDisplay(id: String, box: VecBox) {
        val center = box.center
        val center2d = Vec(center.x(), center.y(), center.z())

        // Create combined region entity (interaction + display)
        val entity = RegionEntity(id)
        entity.setInstance(instance, center2d)
        regionDisplays[id] = entity
    }

    fun cancelCreation(player: Player) {
        creationSessions.remove(player.uuid)
        player.sendActionBar(Component.text("Region creation cancelled", NamedTextColor.GRAY))
    }

    fun hasActiveCreationSession(player: Player): Boolean = creationSessions.containsKey(player.uuid)

    fun getCreationSession(player: Player): RegionSession? = creationSessions[player.uuid]

    fun getRegionEntity(regionId: String): RegionEntity? = regionDisplays[regionId]

    /**
     * Rename a region and update all associated data structures.
     */
    fun renameRegion(oldId: String, newId: String) {
        val region = regions.remove(oldId) ?: return
        val entity = regionDisplays.remove(oldId) ?: return

        regions[newId] = region
        entity.regionId = newId
        entity.updateNametag()

        regionDisplays[newId] = entity
    }

    /**
     * Delete a region and its associated displays.
     */
    fun deleteRegion(regionId: String) {
        regions.remove(regionId)
        regionDisplays[regionId]?.remove()
        regionDisplays.remove(regionId)
    }

    fun tick() {
        particleUpdateTick++
        if (particleUpdateTick % 2 != 0L) return // Update every 2 ticks

        instance.players.forEach { player ->
            if (!isParticleVisualizationEnabled(player)) {
                return@forEach
            }

            visualizeRegions(player)

            creationSessions[player.uuid]?.let { session ->
                visualizeCreationSession(player, session)
            }
        }
    }

    private fun visualizeRegions(player: Player) {
        regions.forEach { (id, box) ->
            visualizeBox(player, box, id, generateColorFromString(id))
        }
    }

    private fun visualizeCreationSession(player: Player, session: RegionSession) {
        session.pos1?.let { pos1 ->
            spawnParticle(player, pos1)

            session.pos2?.let { pos2 ->
                spawnParticle(player, pos2)
                visualizeBox(player, VecBox(pos1, pos2), "current", DustColor.BLUE)
            }
        }
    }

    private fun visualizeBox(player: Player, box: VecBox, label: String, color: DustColor) {
        val min = box.min
        val max = box.max

        // Corner points
        listOf(
            Vector3d(min.x(), min.y(), min.z()),
            Vector3d(max.x(), min.y(), min.z()),
            Vector3d(min.x(), max.y(), min.z()),
            Vector3d(max.x(), max.y(), min.z()),
            Vector3d(min.x(), min.y(), max.z()),
            Vector3d(max.x(), min.y(), max.z()),
            Vector3d(min.x(), max.y(), max.z()),
            Vector3d(max.x(), max.y(), max.z()),
        ).forEach { corner ->
            spawnParticle(player, corner, color)
        }

        // Edges
        val edgePoints = listOf(
            Pair(Vector3d(min.x(), min.y(), min.z()), Vector3d(max.x(), min.y(), min.z())), // bottom front
            Pair(Vector3d(min.x(), min.y(), max.z()), Vector3d(max.x(), min.y(), max.z())), // bottom back
            Pair(Vector3d(min.x(), max.y(), min.z()), Vector3d(max.x(), max.y(), min.z())), // top front
            Pair(Vector3d(min.x(), max.y(), max.z()), Vector3d(max.x(), max.y(), max.z())), // top back
            Pair(Vector3d(min.x(), min.y(), min.z()), Vector3d(min.x(), max.y(), min.z())), // left front
            Pair(Vector3d(max.x(), min.y(), min.z()), Vector3d(max.x(), max.y(), min.z())), // right front
            Pair(Vector3d(min.x(), min.y(), max.z()), Vector3d(min.x(), max.y(), max.z())), // left back
            Pair(Vector3d(max.x(), min.y(), max.z()), Vector3d(max.x(), max.y(), max.z())), // right back
            Pair(Vector3d(min.x(), min.y(), min.z()), Vector3d(min.x(), min.y(), max.z())), // bottom left
            Pair(Vector3d(max.x(), min.y(), min.z()), Vector3d(max.x(), min.y(), max.z())), // bottom right
            Pair(Vector3d(min.x(), max.y(), min.z()), Vector3d(min.x(), max.y(), max.z())), // top left
            Pair(Vector3d(max.x(), max.y(), min.z()), Vector3d(max.x(), max.y(), max.z())), // top right
        )

        edgePoints.forEach { (from, to) ->
            drawLine(player, from, to, color)
        }
    }

    private fun drawLine(player: Player, from: Vector3d, to: Vector3d, color: DustColor, steps: Int = 8) {
        val dx = (to.x - from.x) / steps
        val dy = (to.y - from.y) / steps
        val dz = (to.z - from.z) / steps

        for (i in 0..steps) {
            val point = Vector3d(
                from.x + dx * i,
                from.y + dy * i,
                from.z + dz * i
            )
            spawnParticle(player, point, color)
        }
    }

    private fun spawnParticle(player: Player, position: Vector3dc, color: DustColor? = null) {
        val particle = color?.let { color ->
            Particle.DUST.withColor(Color(color.red, color.green, color.blue))
        } ?: Particle.DUST.withColor(Color.BLACK)

        val packet = ParticlePacket(
            particle,
            position.x(), position.y(), position.z(),
            0.0f, 0.0f, 0.0f,
            0.0f, 1
        )

        player.sendPacket(packet)
    }

    fun collectRegions(root: BlockVec): Map<String, VecBox> {
        return regions.mapValues { (_, box) -> box.offset(Vector3d(-root.x(), -root.y(), -root.z())) }
    }

    /**
     * Represents a region entity combining interaction and text display.
     * Players can click to select/delete the region, and a nametag shows the region ID.
     */
    class RegionEntity(var regionId: String) : Entity(EntityType.INTERACTION) {
        private val nametag: Nametag = Nametag()

        init {
            setBoundingBox(0.5, 0.5, 0.5)

            editEntityMeta(InteractionMeta::class.java) { meta ->
                meta.width = boundingBox.width().toFloat()
                meta.height = boundingBox.height().toFloat()
                meta.response = true
            }

            setNoGravity(true)
            preventBlockPlacement = false
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
                }
            }

            private fun updateName(meta: TextDisplayMeta) {
                val component = Component.text()

                component.append(Component.text("REGION").decorate(TextDecoration.BOLD))
                component.appendNewline()
                component.append(Component.text("Id: ${this@RegionEntity.regionId}"))

                meta.text = component.build()
            }

            override fun updateNewViewer(viewer: Player) {
                val regionEntity = this@RegionEntity
                position = regionEntity.position
                super.updateNewViewer(viewer)
                viewer.sendPacket(SetPassengersPacket(regionEntity.entityId, listOf(entityId)))
            }

            fun updateNametag() {
                editEntityMeta(TextDisplayMeta::class.java) { meta ->
                    updateName(meta)
                }

                this@RegionEntity.sendPacketsToViewers(metadataPacket)
            }
        }
    }



    companion object {
        /**
         * Generates a unique color from a string using its hash code.
         */
        fun generateColorFromString(value: String): DustColor {
            val hash = value.hashCode()
            val r = ((hash shr 16) and 0xFF) / 255f
            val g = ((hash shr 8) and 0xFF) / 255f
            val b = (hash and 0xFF) / 255f

            return DustColor(r, g, b)
        }
    }
}
