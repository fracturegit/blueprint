package net.mcbrawls.blueprint.editor

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.mcbrawls.blueprint.Anchor
import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.PlacedBlueprint
import net.mcbrawls.blueprint.PropertyValue
import net.mcbrawls.blueprint.editor.anchor.MarkerAnchorEntity
import net.mcbrawls.blueprint.editor.anchor.MarkerGroupEntity
import net.mcbrawls.blueprint.editor.region.InstanceRegionHandler
import net.mcbrawls.blueprint.minestom.MinestomBlueprintSerializer
import net.mcbrawls.blueprint.minestom.MinestomBlueprints.combinedPos
import net.mcbrawls.blueprint.util.NbtOps
import net.mcbrawls.codex.encodeQuick
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerPickBlockEvent
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.world.DimensionType
import org.joml.Vector2f
import org.joml.Vector3d
import java.io.File
import java.util.UUID

class BlueprintEditorInstance(val blueprintId: Key, val blueprint: Blueprint<Block>?) : InstanceContainer(UUID.randomUUID(), DimensionType.OVERWORLD) {
    private var initialized: Boolean = false

    private var placedBlueprint: PlacedBlueprint<Block>? = null
    private val bounds = Bounds(ORIGIN)
    private val regionHandler = InstanceRegionHandler(this, ORIGIN, blueprint?.regions ?: emptyMap())

    /** All marker group entities currently in this editor, keyed by marker name. */
    private val markerEntities: MutableMap<String, MarkerGroupEntity> = mutableMapOf()

    fun initializeInternal() {
        if (blueprint != null) {
            val placed = MinestomBlueprintSerializer.placeBlueprint(this, ORIGIN, blueprint)
            placedBlueprint = placed

            placed.getAllMarkers().forEach { (name, marker) ->
                spawnMarker(name, marker)
            }

            regionHandler.initialize()
        } else {
            setBlock(ORIGIN, Block.STONE)
        }
    }

    fun initializeEvents(node: EventNode<InstanceEvent>) {
        node.addListener(RemoveEntityFromInstanceEvent::class.java) { event ->
            val player = event.entity as? Player ?: return@addListener
            regionHandler.removePlayer(player)
            player.removeTag(ACTIVE_MARKER_TAG)
            player.removeTag(ACTIVE_ANCHOR_TAG)
            player.removeTag(ACTIVE_REGION_TAG)
        }

        node.addListener(PlayerChatEvent::class.java) { event ->
            event.isCancelled = true
            val player = event.player
            val str = event.rawMessage

            // --- Region creation mode intercepts all input ---
            if (regionHandler.hasActiveCreationSession(player)) {
                if (str.startsWith("$")) {
                    when (str) {
                        $$"$region cancel" -> regionHandler.cancelCreation(player)
                    }
                } else {
                    if (regionHandler.confirmRegion(player, str)) regionHandler.exitCreationMode(player)
                }
                return@addListener
            }

            // --- Commands ---
            if (str.startsWith("$")) {
                handleCommand(player, str)
                return@addListener
            }

            // --- Plain text: rename active marker ---
            player.getTag(ACTIVE_MARKER_TAG)?.let { currentName ->
                renameMarker(player, currentName, str)
            }
        }

        node.addListener(PlayerEntityInteractEvent::class.java) { event ->
            val player = event.player

            when (val entity = event.target) {
                is InstanceRegionHandler.RegionEntity -> {
                    setActiveRegion(player, entity.regionId)
                }
                is MarkerGroupEntity -> {
                    setActiveMarker(player, entity.markerName)
                }
                is MarkerAnchorEntity -> {
                    setActiveAnchor(player, entity.uuid, entity.markerGroup.markerName)
                }
            }
        }

        node.addListener(EntityAttackEvent::class.java) { event ->
            val player = event.entity as? Player ?: return@addListener

            when (val entity = event.target) {
                is MarkerAnchorEntity -> {
                    removeAnchor(player, entity)
                }
                is MarkerGroupEntity -> {
                    removeMarker(player, entity.markerName)
                }
            }
        }

        node.addListener(PlayerUseItemOnBlockEvent::class.java) { event ->
            if (event.hand == PlayerHand.OFF) return@addListener

            val player = event.player
            val point = event.position.add(event.cursorPosition)
            val playerPos = player.position

            // Region creation takes priority
            if (regionHandler.hasActiveCreationSession(player)) {
                regionHandler.setPosition(player, Vector3d(point.x(), point.y(), point.z()))
                return@addListener
            }

            if (event.itemStack.material() != Material.STICK) return@addListener

            val activeMarkerName = player.getTag(ACTIVE_MARKER_TAG)
            if (activeMarkerName == null) {
                player.sendActionBar(Component.text("Select a marker first (right-click its group entity)", NamedTextColor.YELLOW))
                return@addListener
            }

            val markerEntity = markerEntities[activeMarkerName]
            if (markerEntity == null) {
                player.sendActionBar(Component.text("Marker '$activeMarkerName' not found", NamedTextColor.RED))
                return@addListener
            }

            val anchorPos = Vector3d(point.x(), point.y(), point.z())
            val anchorRot = Vector2f(playerPos.yaw, playerPos.pitch)
            val anchor = Anchor(anchorPos, anchorRot)

            val anchorEntity = MarkerAnchorEntity(markerEntity)
            anchorEntity.setInstance(this, anchor.combinedPos)
            markerEntity.anchorEntities.add(anchorEntity)
            markerEntity.updateNametag()
            anchorEntity.updateNametag()

            player.sendActionBar(Component.text("Added anchor #${markerEntity.anchorEntities.size} to '$activeMarkerName'"))
        }

        node.addListener(PlayerPickBlockEvent::class.java) { event ->
            runCatching {
                val player = event.player
                val heldSlot = player.heldSlot
                val inventory = player.inventory
                val key = event.block.key()
                val material = Material.fromKey(key)
                val stack = net.minestom.server.item.ItemStack.builder(material).build()

                for (i in 0 until 9) {
                    if (inventory.getItemStack(i).isSimilar(stack)) {
                        player.setHeldItemSlot(i.toByte())
                        return@addListener
                    }
                }

                val existingStack = inventory.getItemStack(heldSlot.toInt())
                if (!existingStack.isSimilar(stack)) {
                    inventory.setItemStack(heldSlot.toInt(), stack)
                    if (!existingStack.isAir) inventory.addItemStack(existingStack)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Command handling
    // -------------------------------------------------------------------------

    private fun handleCommand(player: Player, str: String) {
        val parts = str.removePrefix("$").trim().split(" ")
        val command = parts[0]

        when (command) {
            "clear" -> {
                player.removeTag(ACTIVE_MARKER_TAG)
                player.removeTag(ACTIVE_ANCHOR_TAG)
                player.removeTag(ACTIVE_REGION_TAG)
                player.sendActionBar(Component.text("Cleared selection"))
            }

            "remove" -> {
                player.getTag(ACTIVE_ANCHOR_TAG)?.let { uuid ->
                    val anchorEntity = entities.filterIsInstance<MarkerAnchorEntity>()
                        .find { it.uuid == uuid }
                    if (anchorEntity != null) {
                        removeAnchor(player, anchorEntity)
                        return
                    }
                }
                player.getTag(ACTIVE_MARKER_TAG)?.let { name ->
                    removeMarker(player, name)
                    return
                }
                player.getTag(ACTIVE_REGION_TAG)?.let { regionId ->
                    regionHandler.deleteRegion(regionId)
                    player.removeTag(ACTIVE_REGION_TAG)
                    player.sendActionBar(Component.text("Deleted region '$regionId'", NamedTextColor.RED))
                }
            }

            "teleport" -> {
                player.getTag(ACTIVE_ANCHOR_TAG)?.let { uuid ->
                    val entity = entities.filterIsInstance<MarkerAnchorEntity>().find { it.uuid == uuid }
                    entity?.let {
                        player.teleport(it.position.sub(0.0, player.eyeHeight, 0.0).withView(player.position))
                    }
                }
            }

            // $marker <name> [type]  - create a new marker group at player position
            "marker" -> {
                val name = parts.getOrNull(1) ?: run {
                    player.sendActionBar(Component.text("Usage: \$marker <name> [type]", NamedTextColor.RED))
                    return
                }
                if (markerEntities.containsKey(name)) {
                    player.sendActionBar(Component.text("Marker '$name' already exists", NamedTextColor.RED))
                    return
                }
                val type = parts.getOrNull(2)?.let { runCatching { Key.key(it) }.getOrNull() }
                    ?: Key.key("blueprint", "unknown")

                val groupPos = player.position
                val markerEntity = MarkerGroupEntity(name, type)
                markerEntity.setInstance(this, groupPos)
                markerEntities[name] = markerEntity
                setActiveMarker(player, name)
                player.sendActionBar(Component.text("Created marker '$name' ($type)"))
            }

            // $type <key>  - change type of active marker
            "type" -> {
                val activeMarkerName = player.getTag(ACTIVE_MARKER_TAG) ?: run {
                    player.sendActionBar(Component.text("No marker selected", NamedTextColor.RED))
                    return
                }
                val newType = parts.getOrNull(1)?.let { runCatching { Key.key(it) }.getOrNull() } ?: run {
                    player.sendActionBar(Component.text("Usage: \$type <namespace:key>", NamedTextColor.RED))
                    return
                }
                markerEntities[activeMarkerName]?.let { entity ->
                    entity.markerType = newType
                    entity.updateNametag()
                    player.sendActionBar(Component.text("Type set to $newType"))
                }
            }

            // $prop <key> [value]  - set or remove a property on the active marker
            "prop" -> {
                val activeMarkerName = player.getTag(ACTIVE_MARKER_TAG) ?: run {
                    player.sendActionBar(Component.text("No marker selected", NamedTextColor.RED))
                    return
                }
                val key = parts.getOrNull(1) ?: run {
                    player.sendActionBar(Component.text("Usage: \$prop <key> [value]", NamedTextColor.RED))
                    return
                }
                val markerEntity = markerEntities[activeMarkerName] ?: return
                val value = parts.drop(2).joinToString(" ").takeIf { it.isNotEmpty() }

                val updated = markerEntity.properties.toMutableMap()
                if (value != null) {
                    updated[key] = inferPropertyValue(value)
                    player.sendActionBar(Component.text("Set $key = $value"))
                } else {
                    updated.remove(key)
                    player.sendActionBar(Component.text("Removed property '$key'"))
                }
                markerEntity.properties = updated
                markerEntity.updateNametag()
            }

            // $aprop <key> [value]  - set or remove a property on the active anchor
            "aprop" -> {
                val anchorUuid = player.getTag(ACTIVE_ANCHOR_TAG) ?: run {
                    player.sendActionBar(Component.text("No anchor selected", NamedTextColor.RED))
                    return
                }
                val key = parts.getOrNull(1) ?: run {
                    player.sendActionBar(Component.text("Usage: \$aprop <key> [value]", NamedTextColor.RED))
                    return
                }
                val anchorEntity = entities.filterIsInstance<MarkerAnchorEntity>()
                    .find { it.uuid == anchorUuid } ?: return
                val value = parts.drop(2).joinToString(" ").takeIf { it.isNotEmpty() }

                val updated = anchorEntity.properties.toMutableMap()
                if (value != null) {
                    updated[key] = inferPropertyValue(value)
                    player.sendActionBar(Component.text("Anchor: set $key = $value"))
                } else {
                    updated.remove(key)
                    player.sendActionBar(Component.text("Anchor: removed property '$key'"))
                }
                anchorEntity.properties = updated
                anchorEntity.updateNametag()
            }

            // Region sub-commands (unchanged)
            "region" -> {
                when (parts.getOrNull(1)) {
                    "create" -> regionHandler.enterCreationMode(player)
                    "exit" -> regionHandler.exitCreationMode(player)
                    "toggle" -> {
                        val enabled = regionHandler.togglePlayerParticleVisualization(player)
                        player.sendActionBar(Component.text("Region particles ${if (enabled) "enabled" else "disabled"}"))
                    }
                    else -> player.sendActionBar(Component.text("Unknown region command", NamedTextColor.RED))
                }
            }

            else -> player.sendActionBar(Component.text("Unknown command: \$$command", NamedTextColor.RED))
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun spawnMarker(name: String, marker: net.mcbrawls.blueprint.Marker) {
        val groupPos: Pos = if (marker.anchors.isNotEmpty()) {
            val cx = marker.anchors.sumOf { it.position.x() } / marker.anchors.size
            val cy = marker.anchors.sumOf { it.position.y() } / marker.anchors.size
            val cz = marker.anchors.sumOf { it.position.z() } / marker.anchors.size
            Pos(cx, cy, cz)
        } else {
            ORIGIN.asPos()
        }

        val markerEntity = MarkerGroupEntity(name, marker.type, marker.properties)
        markerEntity.setInstance(this, groupPos)
        markerEntities[name] = markerEntity

        marker.anchors.forEach { anchor ->
            val anchorEntity = MarkerAnchorEntity(markerEntity, anchor.properties)
            anchorEntity.setInstance(this, anchor.combinedPos)
            markerEntity.anchorEntities.add(anchorEntity)
            anchorEntity.updateNametag()
        }

        markerEntity.updateNametag()
    }

    private fun removeMarker(player: Player, name: String) {
        val markerEntity = markerEntities.remove(name) ?: return
        markerEntity.anchorEntities.toList().forEach { it.remove() }
        markerEntity.anchorEntities.clear()
        markerEntity.remove()
        if (player.getTag(ACTIVE_MARKER_TAG) == name) {
            player.removeTag(ACTIVE_MARKER_TAG)
            player.removeTag(ACTIVE_ANCHOR_TAG)
        }
        player.sendActionBar(Component.text("Removed marker '$name'", NamedTextColor.RED))
    }

    private fun removeAnchor(player: Player, anchorEntity: MarkerAnchorEntity) {
        val group = anchorEntity.markerGroup
        anchorEntity.remove()
        group.anchorEntities.remove(anchorEntity)
        group.updateNametag()
        if (player.getTag(ACTIVE_ANCHOR_TAG) == anchorEntity.uuid) {
            player.removeTag(ACTIVE_ANCHOR_TAG)
        }
        player.sendActionBar(Component.text("Removed anchor from '${group.markerName}'"))
    }

    private fun renameMarker(player: Player, currentName: String, newName: String) {
        if (markerEntities.containsKey(newName)) {
            player.sendActionBar(Component.text("'$newName' already exists", NamedTextColor.RED))
            return
        }
        val entity = markerEntities.remove(currentName) ?: return
        entity.markerName = newName
        entity.updateNametag()
        entity.anchorEntities.forEach { it.updateNametag() }
        markerEntities[newName] = entity
        player.setTag(ACTIVE_MARKER_TAG, newName)
        player.sendActionBar(Component.text("Renamed '$currentName' → '$newName'"))
    }

    private fun setActiveMarker(player: Player, name: String) {
        if (player.getTag(ACTIVE_MARKER_TAG) == name) return
        player.removeTag(ACTIVE_ANCHOR_TAG)
        player.removeTag(ACTIVE_REGION_TAG)
        player.setTag(ACTIVE_MARKER_TAG, name)
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1.0f, 1.0f))
    }

    private fun setActiveAnchor(player: Player, uuid: UUID, markerName: String) {
        if (player.getTag(ACTIVE_ANCHOR_TAG) == uuid) return
        player.removeTag(ACTIVE_REGION_TAG)
        player.setTag(ACTIVE_ANCHOR_TAG, uuid)
        player.setTag(ACTIVE_MARKER_TAG, markerName)
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1.0f, 1.0f))
    }

    private fun setActiveRegion(player: Player, regionId: String) {
        if (player.getTag(ACTIVE_REGION_TAG) == regionId) return
        player.removeTag(ACTIVE_MARKER_TAG)
        player.removeTag(ACTIVE_ANCHOR_TAG)
        player.setTag(ACTIVE_REGION_TAG, regionId)
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1.0f, 1.0f))
    }

    /**
     * Infers the most specific [PropertyValue] type from a string value.
     * Tries Boolean (strict), then Int, then Double, then falls back to String.
     */
    private fun inferPropertyValue(value: String): PropertyValue = when {
        value.toBooleanStrictOrNull() != null -> PropertyValue.BoolValue(value.toBooleanStrict())
        value.toIntOrNull() != null -> PropertyValue.IntValue(value.toInt())
        value.toDoubleOrNull() != null -> PropertyValue.DoubleValue(value.toDouble())
        else -> PropertyValue.StringValue(value)
    }

    fun postInitialize() {
        initialized = true
    }

    override fun tick(time: Long) {
        super.tick(time)
        if (!initialized) return

        regionHandler.tick()

        if (players.isEmpty()) {
            BlueprintEditorHandler.remove(this)
            return
        }

        players.forEach { player ->
            player.getTag(ACTIVE_MARKER_TAG)?.let { name ->
                val anchor = player.getTag(ACTIVE_ANCHOR_TAG)
                if (anchor != null) {
                    player.sendActionBar(Component.text("Anchor #${markerEntities[name]?.anchorEntities?.indexOfFirst { it.uuid == anchor }?.plus(1) ?: "?"} in '$name' - \$aprop/\$remove/\$teleport/\$clear"))
                } else {
                    player.sendActionBar(Component.text("Marker: $name - type in chat to rename, \$type/\$prop/\$remove"))
                }
            }

            player.getTag(ACTIVE_REGION_TAG)?.let { regionId ->
                player.sendActionBar(Component.text("Modifying region: $regionId"))
            }
        }
    }

    override fun setBlock(x: Int, y: Int, z: Int, block: Block, doBlockUpdates: Boolean) {
        super.setBlock(x, y, z, block, doBlockUpdates)
        bounds.update(x, y, z)
    }

    fun save(folder: File) {
        val path = "${blueprintId.namespace()}/${blueprintId.value()}"
        val file = folder.resolve("$path.nbt")

        val root = bounds.min
        val blockMap = MinestomBlueprintHelper.getBlocks(this, bounds)
        val regions = regionHandler.collectRegions(root)
        val markerGroups = entities.filterIsInstance<MarkerGroupEntity>()
        val blueprint = MinestomBlueprintHelper.createBlueprint(root, blockMap, markerGroups, regions)

        val tag = MinestomBlueprintSerializer.CODEC.encodeQuick(NbtOps.INSTANCE, blueprint)
        if (tag is CompoundBinaryTag) {
            file.parentFile.mkdirs()
            file.outputStream().use {
                BinaryTagIO.writer().write(tag, it, BinaryTagIO.Compression.GZIP)
            }
        }
    }

    companion object {
        val ORIGIN = BlockVec(0, 100, 0)

        val ACTIVE_MARKER_TAG: Tag<String> = Tag.String("active_marker")
        val ACTIVE_ANCHOR_TAG: Tag<UUID> = Tag.UUID("active_anchor")
        val ACTIVE_REGION_TAG: Tag<String> = Tag.String("active_region")
    }
}
