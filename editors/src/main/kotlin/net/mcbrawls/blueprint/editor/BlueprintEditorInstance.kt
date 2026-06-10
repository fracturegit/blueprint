package net.mcbrawls.blueprint.editor

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.mcbrawls.blueprint.Anchor
import net.mcbrawls.blueprint.BlockDatum
import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.CardinalDirection
import net.mcbrawls.blueprint.ConnectorType
import net.mcbrawls.blueprint.Decoration
import net.mcbrawls.blueprint.PlacedBlueprint
import net.mcbrawls.blueprint.PropertyValue
import net.mcbrawls.blueprint.RoomConnector
import net.mcbrawls.blueprint.Waypoint
import net.mcbrawls.blueprint.camera.CameraTrack
import net.mcbrawls.blueprint.camera.EasingFunction
import net.mcbrawls.blueprint.camera.PathMode
import net.mcbrawls.blueprint.editor.anchor.MarkerAnchorEntity
import net.mcbrawls.blueprint.editor.anchor.MarkerGroupEntity
import net.mcbrawls.blueprint.editor.camera.CameraKeyframeEntity
import net.mcbrawls.blueprint.editor.camera.CameraTrackEntity
import net.mcbrawls.blueprint.editor.connector.ConnectorMarkerEntity
import net.mcbrawls.blueprint.editor.decoration.DecorationEntity
import net.mcbrawls.blueprint.editor.region.InstanceRegionHandler
import net.mcbrawls.blueprint.editor.waypoint.WaypointEntity
import net.mcbrawls.blueprint.minestom.MinestomBlueprintSerializer
import net.mcbrawls.blueprint.minestom.MinestomBlueprints.combinedPos
import net.mcbrawls.blueprint.util.NbtOps
import net.mcbrawls.codex.encodeQuick
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerPickBlockEvent
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.world.DimensionType
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3i
import java.io.File
import java.util.UUID

class BlueprintEditorInstance(var blueprintId: Key, val blueprint: Blueprint<Block>?) : InstanceContainer(UUID.randomUUID(), DimensionType.OVERWORLD) {
    private var initialized: Boolean = false

    private var placedBlueprint: PlacedBlueprint<Block>? = null
    private val bounds = Bounds(ORIGIN)
    internal val regionHandler = InstanceRegionHandler(this, ORIGIN, blueprint?.regions ?: emptyMap())

    /** All marker group entities currently in this editor, keyed by marker name. */
    internal val markerEntities: MutableMap<String, MarkerGroupEntity> = mutableMapOf()
    internal val waypointEntities: MutableMap<String, WaypointEntity> = mutableMapOf()
    internal val trackEntities: MutableMap<String, CameraTrackEntity> = mutableMapOf()

    /** All connector entities currently in this editor. Order is not significant. */
    internal val connectorEntities: MutableList<ConnectorMarkerEntity> = mutableListOf()
    internal val decorationEntities: MutableList<DecorationEntity> = mutableListOf()

    /** Block-position (world coords) → mutable property map for blocks with custom data. */
    internal val blockDataMap: MutableMap<Vector3i, MutableMap<String, PropertyValue>> = mutableMapOf()

    fun initializeInternal() {
        if (blueprint != null) {
            val placed = MinestomBlueprintSerializer.placeBlueprint(this, ORIGIN, blueprint)
            placedBlueprint = placed

            blueprint.blockData.forEach { datum ->
                val worldPos = Vector3i(
                    ORIGIN.blockX + datum.position.x(),
                    ORIGIN.blockY + datum.position.y(),
                    ORIGIN.blockZ + datum.position.z(),
                )
                blockDataMap[worldPos] = datum.properties.toMutableMap()
            }

            placed.getAllMarkers().forEach { (name, marker) ->
                spawnMarker(name, marker)
            }

            placed.getAllWaypoints().forEach { (name, waypoint) ->
                spawnWaypoint(name, waypoint)
            }

            placed.getAllCameraTracks().forEach { (id, track) ->
                spawnCameraTrack(id, track)
            }

            // Connectors: local positions are offset by ORIGIN to get world positions.
            blueprint.connectors.forEach { connector ->
                spawnConnector(connector)
            }

            // Decorations: local positions are offset by ORIGIN to get world positions.
            blueprint.decorations.forEach { decoration ->
                spawnDecoration(decoration)
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
            player.removeTag(ACTIVE_WAYPOINT_TAG)
            player.removeTag(ACTIVE_TRACK_TAG)
            player.removeTag(ACTIVE_KEYFRAME_TAG)
            player.removeTag(ACTIVE_CONNECTOR_TAG)
            player.removeTag(ACTIVE_DECORATION_TAG)
            player.removeTag(ACTIVE_BLOCK_TAG)
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
                is WaypointEntity -> {
                    entity.teleportPlayer(player)
                    player.removeTag(ACTIVE_MARKER_TAG)
                    player.removeTag(ACTIVE_ANCHOR_TAG)
                    player.removeTag(ACTIVE_REGION_TAG)
                    player.removeTag(ACTIVE_TRACK_TAG)
                    player.removeTag(ACTIVE_KEYFRAME_TAG)
                    player.removeTag(ACTIVE_CONNECTOR_TAG)
                    player.setTag(ACTIVE_WAYPOINT_TAG, entity.waypointName)
                    player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1f, 1f))
                }
                is CameraTrackEntity -> {
                    player.removeTag(ACTIVE_MARKER_TAG); player.removeTag(ACTIVE_ANCHOR_TAG)
                    player.removeTag(ACTIVE_REGION_TAG); player.removeTag(ACTIVE_WAYPOINT_TAG)
                    player.removeTag(ACTIVE_KEYFRAME_TAG); player.removeTag(ACTIVE_CONNECTOR_TAG)
                    player.setTag(ACTIVE_TRACK_TAG, entity.trackId)
                    player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1f, 1f))
                }
                is CameraKeyframeEntity -> {
                    player.removeTag(ACTIVE_MARKER_TAG); player.removeTag(ACTIVE_ANCHOR_TAG)
                    player.removeTag(ACTIVE_REGION_TAG); player.removeTag(ACTIVE_WAYPOINT_TAG)
                    player.removeTag(ACTIVE_CONNECTOR_TAG)
                    player.setTag(ACTIVE_TRACK_TAG, entity.trackEntity.trackId)
                    player.setTag(ACTIVE_KEYFRAME_TAG, entity.index)
                    player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1f, 1f))
                }
                is ConnectorMarkerEntity -> {
                    // Select the connector — store its index in the list as the active tag.
                    val index = connectorEntities.indexOf(entity)
                    if (index >= 0) {
                        player.removeTag(ACTIVE_MARKER_TAG); player.removeTag(ACTIVE_ANCHOR_TAG)
                        player.removeTag(ACTIVE_REGION_TAG); player.removeTag(ACTIVE_WAYPOINT_TAG)
                        player.removeTag(ACTIVE_TRACK_TAG); player.removeTag(ACTIVE_KEYFRAME_TAG)
                        player.setTag(ACTIVE_CONNECTOR_TAG, index)
                        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1f, 1f))
                    }
                }
                is DecorationEntity -> {
                    player.removeTag(ACTIVE_MARKER_TAG); player.removeTag(ACTIVE_ANCHOR_TAG)
                    player.removeTag(ACTIVE_REGION_TAG); player.removeTag(ACTIVE_WAYPOINT_TAG)
                    player.removeTag(ACTIVE_TRACK_TAG); player.removeTag(ACTIVE_KEYFRAME_TAG)
                    player.removeTag(ACTIVE_CONNECTOR_TAG)
                    player.setTag(ACTIVE_DECORATION_TAG, entity.entityId)
                    player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1f, 1f))
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
                is WaypointEntity -> {
                    waypointEntities.remove(entity.waypointName)
                    entity.remove()
                    if (player.getTag(ACTIVE_WAYPOINT_TAG) == entity.waypointName) {
                        player.removeTag(ACTIVE_WAYPOINT_TAG)
                    }
                    player.sendActionBar(Component.text("Removed waypoint '${entity.waypointName}'", NamedTextColor.RED))
                }
                is CameraTrackEntity -> {
                    removeTrack(player, entity.trackId)
                }
                is CameraKeyframeEntity -> {
                    removeKeyframe(player, entity)
                }
                is ConnectorMarkerEntity -> {
                    removeConnectorEntity(player, entity)
                }
                is DecorationEntity -> {
                    removeDecorationEntity(player, entity)
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

            // Feather: select block for data editing
            if (event.itemStack.material() == Material.FEATHER) {
                val bp = event.position
                val worldPos = Vector3i(bp.blockX(), bp.blockY(), bp.blockZ())
                player.removeTag(ACTIVE_MARKER_TAG)
                player.removeTag(ACTIVE_ANCHOR_TAG)
                player.removeTag(ACTIVE_REGION_TAG)
                player.removeTag(ACTIVE_WAYPOINT_TAG)
                player.removeTag(ACTIVE_TRACK_TAG)
                player.removeTag(ACTIVE_KEYFRAME_TAG)
                player.removeTag(ACTIVE_CONNECTOR_TAG)
                player.removeTag(ACTIVE_DECORATION_TAG)
                player.setTag(ACTIVE_BLOCK_TAG, "${worldPos.x()},${worldPos.y()},${worldPos.z()}")
                player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1f, 1f))
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
    // Connector editing
    // -------------------------------------------------------------------------

    internal fun handleConnectorCommand(player: Player, parts: List<String>) {
        when (val sub = parts.getOrNull(1)) {
            "add" -> {
                val dir = parts.getOrNull(2)
                    ?.uppercase()
                    ?.let { runCatching { CardinalDirection.valueOf(it) }.getOrNull() }
                    ?: run {
                        player.sendActionBar(Component.text(
                            "Usage: \$connector add <north|south|east|west> <entrance|exit>",
                            NamedTextColor.RED
                        ))
                        return
                    }
                val type = parts.getOrNull(3)
                    ?.uppercase()
                    ?.let { runCatching { ConnectorType.valueOf(it) }.getOrNull() }
                    ?: run {
                        player.sendActionBar(Component.text(
                            "Usage: \$connector add <north|south|east|west> <entrance|exit>",
                            NamedTextColor.RED
                        ))
                        return
                    }

                // Local position = player block position minus blueprint origin.
                val origin = ORIGIN
                val localPos = Vector3i(
                    player.position.blockX() - origin.blockX(),
                    player.position.blockY() - origin.blockY(),
                    player.position.blockZ() - origin.blockZ(),
                )
                val entity = ConnectorMarkerEntity(localPos, dir, type)
                val worldPos = Pos(
                    player.position.blockX() + 0.5,
                    player.position.blockY().toDouble(),
                    player.position.blockZ() + 0.5,
                )
                entity.setInstance(this, worldPos)
                entity.updateNametag()
                connectorEntities.add(entity)

                // Select the new connector immediately.
                val index = connectorEntities.indexOf(entity)
                player.removeTag(ACTIVE_MARKER_TAG); player.removeTag(ACTIVE_ANCHOR_TAG)
                player.removeTag(ACTIVE_REGION_TAG); player.removeTag(ACTIVE_WAYPOINT_TAG)
                player.removeTag(ACTIVE_TRACK_TAG); player.removeTag(ACTIVE_KEYFRAME_TAG)
                player.setTag(ACTIVE_CONNECTOR_TAG, index)

                val typeLabel = type.name.lowercase()
                val dirLabel = dir.name.lowercase()
                player.sendActionBar(Component.text("Added $typeLabel connector facing $dirLabel at local $localPos"))
            }

            "remove" -> {
                val index = player.getTag(ACTIVE_CONNECTOR_TAG) ?: run {
                    player.sendActionBar(Component.text(
                        "No connector selected — right-click one to select it",
                        NamedTextColor.RED
                    ))
                    return
                }
                val entity = connectorEntities.getOrNull(index) ?: run {
                    player.sendActionBar(Component.text("Connector not found", NamedTextColor.RED))
                    return
                }
                removeConnectorEntity(player, entity)
            }

            "list" -> {
                if (connectorEntities.isEmpty()) {
                    player.sendActionBar(Component.text("No connectors placed"))
                    return
                }
                // Print all connectors to chat since action bar only shows one line.
                connectorEntities.forEachIndexed { i, entity ->
                    val color = if (entity.type == ConnectorType.ENTRANCE) NamedTextColor.GREEN else NamedTextColor.GOLD
                    player.sendMessage(Component.text(
                        "#${i + 1}: ${entity.type.name.lowercase()} facing ${entity.direction.name.lowercase()} at ${entity.localPosition.x()},${entity.localPosition.y()},${entity.localPosition.z()}",
                        color
                    ))
                }
            }

            null -> {
                player.sendActionBar(Component.text(
                    "\$connector add <dir> <entrance|exit>  |  \$connector remove  |  \$connector list",
                    NamedTextColor.YELLOW
                ))
            }

            else -> player.sendActionBar(Component.text("Unknown connector sub-command: $sub", NamedTextColor.RED))
        }
    }

    /**
     * Spawns a [ConnectorMarkerEntity] for a [RoomConnector] that was loaded from the blueprint.
     * The connector's [localPosition] is relative to blueprint origin; we offset it to get world pos.
     */
    private fun spawnConnector(connector: RoomConnector) {
        val origin = ORIGIN
        val worldPos = Pos(
            origin.blockX() + connector.position.x() + 0.5,
            (origin.blockY() + connector.position.y()).toDouble(),
            origin.blockZ() + connector.position.z() + 0.5,
        )
        val entity = ConnectorMarkerEntity.fromConnector(connector)
        entity.setInstance(this, worldPos)
        entity.updateNametag()
        connectorEntities.add(entity)
    }

    private fun removeConnectorEntity(player: Player, entity: ConnectorMarkerEntity) {
        val index = connectorEntities.indexOf(entity)
        entity.remove()
        connectorEntities.remove(entity)
        if (player.getTag(ACTIVE_CONNECTOR_TAG) == index) {
            player.removeTag(ACTIVE_CONNECTOR_TAG)
        }
        val typeLabel = entity.type.name.lowercase()
        val dirLabel = entity.direction.name.lowercase()
        player.sendActionBar(Component.text("Removed $typeLabel connector facing $dirLabel", NamedTextColor.RED))
    }

    // -------------------------------------------------------------------------
    // Decoration editing
    // -------------------------------------------------------------------------

    internal fun handleDecorationCommand(player: Player, parts: List<String>) {
        when (val sub = parts.getOrNull(1)) {
            "add" -> {
                val typeKey = parts.getOrNull(2)?.let { raw ->
                    runCatching {
                        if (':' in raw) Key.key(raw) else Key.key("fracture", raw)
                    }.getOrNull()
                } ?: run {
                    player.sendActionBar(Component.text("Usage: \$decoration add <type_key>", NamedTextColor.RED))
                    return
                }
                val decoration = Decoration(
                    type = typeKey,
                    position = Vector3d(
                        player.position.x - ORIGIN.x(),
                        player.position.y - ORIGIN.y(),
                        player.position.z - ORIGIN.z(),
                    ),
                    rotation = Vector2f(player.position.yaw, player.position.pitch),
                )
                val entity = spawnDecoration(decoration)
                player.removeTag(ACTIVE_MARKER_TAG); player.removeTag(ACTIVE_ANCHOR_TAG)
                player.removeTag(ACTIVE_REGION_TAG); player.removeTag(ACTIVE_WAYPOINT_TAG)
                player.removeTag(ACTIVE_TRACK_TAG); player.removeTag(ACTIVE_KEYFRAME_TAG)
                player.removeTag(ACTIVE_CONNECTOR_TAG)
                player.setTag(ACTIVE_DECORATION_TAG, entity.entityId)
                player.sendActionBar(Component.text("Added decoration '${typeKey.asString()}' (#${decorationEntities.size})"))
            }

            "remove" -> {
                val activeId = player.getTag(ACTIVE_DECORATION_TAG) ?: run {
                    player.sendActionBar(Component.text("No decoration selected — right-click one to select it", NamedTextColor.RED))
                    return
                }
                val entity = decorationEntities.find { it.entityId == activeId } ?: run {
                    player.sendActionBar(Component.text("Decoration not found", NamedTextColor.RED))
                    return
                }
                removeDecorationEntity(player, entity)
            }

            "prop" -> {
                val activeId = player.getTag(ACTIVE_DECORATION_TAG) ?: run {
                    player.sendActionBar(Component.text("No decoration selected", NamedTextColor.RED))
                    return
                }
                val key = parts.getOrNull(2) ?: run {
                    player.sendActionBar(Component.text("Usage: \$decoration prop <key> [value]", NamedTextColor.RED))
                    return
                }
                val entity = decorationEntities.find { it.entityId == activeId } ?: return
                val value = parts.drop(3).joinToString(" ").takeIf { it.isNotEmpty() }
                val updated = entity.properties.toMutableMap()
                if (value != null) {
                    updated[key] = inferPropertyValue(value)
                    player.sendActionBar(Component.text("Decoration: set $key = $value"))
                } else {
                    updated.remove(key)
                    player.sendActionBar(Component.text("Decoration: removed property '$key'"))
                }
                entity.properties = updated
                entity.updateNametag()
            }

            "list" -> {
                if (decorationEntities.isEmpty()) {
                    player.sendActionBar(Component.text("No decorations placed"))
                    return
                }
                decorationEntities.forEachIndexed { i, entity ->
                    val lx = entity.position.blockX() - ORIGIN.x()
                    val ly = entity.position.blockY() - ORIGIN.y()
                    val lz = entity.position.blockZ() - ORIGIN.z()
                    player.sendMessage(Component.text(
                        "#${i + 1}: ${entity.decorationType.asString()} at $lx,$ly,$lz",
                        NamedTextColor.AQUA
                    ))
                }
            }

            null -> {
                player.sendActionBar(Component.text(
                    "\$decoration add <type_key>  |  \$decoration remove  |  \$decoration prop <key> [val]  |  \$decoration list",
                    NamedTextColor.YELLOW
                ))
            }

            else -> player.sendActionBar(Component.text("Unknown decoration sub-command: $sub", NamedTextColor.RED))
        }
    }

    private fun spawnDecoration(decoration: Decoration): DecorationEntity {
        val renderer = BlueprintEditorHandler.decorationRenderer
        val entity = DecorationEntity(decoration.type, decoration.properties)
        val worldPos = Pos(
            decoration.position.x() + ORIGIN.x(),
            decoration.position.y() + ORIGIN.y(),
            decoration.position.z() + ORIGIN.z(),
            decoration.rotation.x(),
            decoration.rotation.y(),
        )
        entity.setInstance(this, worldPos)
        entity.updateNametag()

        val preview = renderer?.spawnPreview(this, decoration)
        if (preview != null) {
            preview.setInstance(this, worldPos)
            entity.addPassenger(preview)
            entity.previewEntity = preview
        }

        decorationEntities.add(entity)
        return entity
    }

    private fun removeDecorationEntity(player: Player, entity: DecorationEntity) {
        entity.previewEntity?.let { BlueprintEditorHandler.decorationRenderer?.removePreview(it) }
        entity.remove()
        decorationEntities.remove(entity)
        if (player.getTag(ACTIVE_DECORATION_TAG) == entity.entityId) {
            player.removeTag(ACTIVE_DECORATION_TAG)
        }
        player.sendActionBar(Component.text("Removed decoration '${entity.decorationType.asString()}'", NamedTextColor.RED))
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

    private fun spawnWaypoint(name: String, waypoint: Waypoint) {
        val entity = WaypointEntity(name)
        entity.setInstance(this, Pos(
            waypoint.position.x(), waypoint.position.y(), waypoint.position.z(),
            waypoint.rotation.x(), waypoint.rotation.y()
        ))
        waypointEntities[name] = entity
    }

    private fun spawnCameraTrack(id: String, track: CameraTrack) {
        val trackEntity = CameraTrackEntity(id)
        val kfEntities = track.keyframes.map { kf ->
            CameraKeyframeEntity(trackEntity, kf.duration, kf.pathMode, kf.easing).also { entity ->
                entity.setInstance(this, Pos(
                    kf.position.x(), kf.position.y(), kf.position.z(),
                    kf.rotation.x(), kf.rotation.y()
                ))
            }
        }
        trackEntity.keyframeEntities.addAll(kfEntities)
        kfEntities.forEach { it.updateNametag() }

        val spawnPos = kfEntities.firstOrNull()?.position?.add(0.75, 0.0, 0.0)
            ?: Pos(ORIGIN.x().toDouble(), ORIGIN.y().toDouble(), ORIGIN.z().toDouble())
        trackEntity.setInstance(this, spawnPos)
        trackEntity.updateNametag()
        trackEntities[id] = trackEntity
    }

    internal fun removeMarker(player: Player, name: String) {
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

    internal fun removeAnchor(player: Player, anchorEntity: MarkerAnchorEntity) {
        val group = anchorEntity.markerGroup
        anchorEntity.remove()
        group.anchorEntities.remove(anchorEntity)
        group.updateNametag()
        if (player.getTag(ACTIVE_ANCHOR_TAG) == anchorEntity.uuid) {
            player.removeTag(ACTIVE_ANCHOR_TAG)
        }
        player.sendActionBar(Component.text("Removed anchor from '${group.markerName}'"))
    }

    internal fun handleTrackCommand(player: Player, parts: List<String>) {
        val sub = parts.getOrNull(1)
        val knownSubs = listOf("add", "remove", "duration", "pathmode", "easing", "teleport", "position", "up", "down", "move")

        if (sub != null && sub !in knownSubs) {
            val id = sub
            if (!trackEntities.containsKey(id)) {
                val trackEntity = CameraTrackEntity(id)
                trackEntity.setInstance(this, player.position)
                trackEntities[id] = trackEntity
                player.sendActionBar(Component.text("Created track '$id'"))
            } else {
                player.sendActionBar(Component.text("Selected track '$id'"))
            }
            player.removeTag(ACTIVE_MARKER_TAG); player.removeTag(ACTIVE_ANCHOR_TAG)
            player.removeTag(ACTIVE_REGION_TAG); player.removeTag(ACTIVE_WAYPOINT_TAG)
            player.removeTag(ACTIVE_KEYFRAME_TAG); player.removeTag(ACTIVE_CONNECTOR_TAG)
            player.setTag(ACTIVE_TRACK_TAG, id)
            return
        }

        val trackId = player.getTag(ACTIVE_TRACK_TAG) ?: run {
            player.sendActionBar(Component.text("No track selected — use \$track <id>", NamedTextColor.RED))
            return
        }
        val trackEntity = trackEntities[trackId] ?: run {
            player.sendActionBar(Component.text("Track '$trackId' not found", NamedTextColor.RED))
            return
        }

        when (sub) {
            "add" -> {
                val kfe = CameraKeyframeEntity(trackEntity)
                kfe.setInstance(this, player.position)
                trackEntity.keyframeEntities.add(kfe)
                kfe.updateNametag()
                trackEntity.updateNametag()
                if (trackEntity.keyframeEntities.size == 1) trackEntity.repositionFromKeyframes()
                player.setTag(ACTIVE_KEYFRAME_TAG, kfe.index)
                player.sendActionBar(Component.text("Added keyframe #${kfe.index + 1} to '$trackId'"))
            }

            "remove" -> {
                val kfIndex = player.getTag(ACTIVE_KEYFRAME_TAG)
                if (kfIndex != null) {
                    val kfe = trackEntity.keyframeEntities.getOrNull(kfIndex)
                    if (kfe != null) { removeKeyframe(player, kfe); return }
                }
                removeTrack(player, trackId)
            }

            "duration" -> {
                val kfIndex = player.getTag(ACTIVE_KEYFRAME_TAG) ?: run {
                    player.sendActionBar(Component.text("No keyframe selected", NamedTextColor.RED)); return
                }
                val value = parts.getOrNull(2)?.toDoubleOrNull() ?: run {
                    player.sendActionBar(Component.text("Usage: \$track duration <seconds>", NamedTextColor.RED)); return
                }
                val kfe = trackEntity.keyframeEntities.getOrNull(kfIndex) ?: return
                kfe.duration = value
                kfe.updateNametag()
                trackEntity.updateNametag()
                player.sendActionBar(Component.text("Duration set to ${value}s"))
            }

            "pathmode" -> {
                val kfIndex = player.getTag(ACTIVE_KEYFRAME_TAG) ?: run {
                    player.sendActionBar(Component.text("No keyframe selected", NamedTextColor.RED)); return
                }
                val mode = parts.getOrNull(2)?.uppercase()?.let { runCatching { PathMode.valueOf(it) }.getOrNull() } ?: run {
                    player.sendActionBar(Component.text("Usage: \$track pathmode <linear|catmull_rom>", NamedTextColor.RED)); return
                }
                val kfe = trackEntity.keyframeEntities.getOrNull(kfIndex) ?: return
                kfe.pathMode = mode
                kfe.updateNametag()
                player.sendActionBar(Component.text("Path mode set to ${mode.name}"))
            }

            "easing" -> {
                val kfIndex = player.getTag(ACTIVE_KEYFRAME_TAG) ?: run {
                    player.sendActionBar(Component.text("No keyframe selected", NamedTextColor.RED)); return
                }
                val fn = parts.getOrNull(2)?.uppercase()?.let { runCatching { EasingFunction.valueOf(it) }.getOrNull() } ?: run {
                    player.sendActionBar(Component.text("Usage: \$track easing <function>", NamedTextColor.RED)); return
                }
                val kfe = trackEntity.keyframeEntities.getOrNull(kfIndex) ?: return
                kfe.easing = fn
                kfe.updateNametag()
                player.sendActionBar(Component.text("Easing set to ${fn.name}"))
            }

            "teleport" -> {
                val kfIndex = player.getTag(ACTIVE_KEYFRAME_TAG) ?: run {
                    player.sendActionBar(Component.text("No keyframe selected", NamedTextColor.RED)); return
                }
                val kfe = trackEntity.keyframeEntities.getOrNull(kfIndex) ?: return
                player.teleport(kfe.position.sub(0.0, player.eyeHeight, 0.0).withView(kfe.position))
            }

            "position" -> {
                val kfIndex = player.getTag(ACTIVE_KEYFRAME_TAG) ?: run {
                    player.sendActionBar(Component.text("No keyframe selected", NamedTextColor.RED)); return
                }
                val kfe = trackEntity.keyframeEntities.getOrNull(kfIndex) ?: return
                kfe.teleport(player.position)
                kfe.updateNametag()
                if (kfIndex == 0) trackEntity.repositionFromKeyframes()
                refreshAllKeyframeNametags(trackEntity)
                player.sendActionBar(Component.text("Keyframe #${kfIndex + 1} moved to current position"))
            }

            "up" -> shiftKeyframe(player, trackEntity, -1)
            "down" -> shiftKeyframe(player, trackEntity, +1)

            "move" -> {
                val kfIndex = player.getTag(ACTIVE_KEYFRAME_TAG) ?: run {
                    player.sendActionBar(Component.text("No keyframe selected", NamedTextColor.RED)); return
                }
                val target = parts.getOrNull(2)?.toIntOrNull()?.minus(1) ?: run {
                    player.sendActionBar(Component.text("Usage: \$track move <index>", NamedTextColor.RED)); return
                }
                val clamped = target.coerceIn(0, trackEntity.keyframeEntities.size - 1)
                val kfe = trackEntity.keyframeEntities.removeAt(kfIndex)
                trackEntity.keyframeEntities.add(clamped, kfe)
                player.setTag(ACTIVE_KEYFRAME_TAG, clamped)
                refreshAllKeyframeNametags(trackEntity)
                if (kfIndex == 0 || clamped == 0) trackEntity.repositionFromKeyframes()
                player.sendActionBar(Component.text("Moved keyframe to position #${clamped + 1}"))
            }

            else -> player.sendActionBar(Component.text("Unknown track sub-command: $sub", NamedTextColor.RED))
        }
    }

    private fun removeTrack(player: Player, trackId: String) {
        val entity = trackEntities.remove(trackId) ?: return
        entity.keyframeEntities.toList().forEach { it.remove() }
        entity.keyframeEntities.clear()
        entity.remove()
        if (player.getTag(ACTIVE_TRACK_TAG) == trackId) {
            player.removeTag(ACTIVE_TRACK_TAG)
            player.removeTag(ACTIVE_KEYFRAME_TAG)
        }
        player.sendActionBar(Component.text("Removed track '$trackId'", NamedTextColor.RED))
    }

    private fun removeKeyframe(player: Player, kfe: CameraKeyframeEntity) {
        val trackEntity = kfe.trackEntity
        val removedIndex = kfe.index  // capture before removal — indexOf returns -1 after
        val wasFirst = removedIndex == 0
        kfe.remove()
        trackEntity.keyframeEntities.remove(kfe)
        if (player.getTag(ACTIVE_KEYFRAME_TAG) == removedIndex) player.removeTag(ACTIVE_KEYFRAME_TAG)
        refreshAllKeyframeNametags(trackEntity)
        trackEntity.updateNametag()
        if (wasFirst) trackEntity.repositionFromKeyframes()
        player.sendActionBar(Component.text("Removed keyframe from '${trackEntity.trackId}'"))
    }

    private fun shiftKeyframe(player: Player, trackEntity: CameraTrackEntity, delta: Int) {
        val kfIndex = player.getTag(ACTIVE_KEYFRAME_TAG) ?: return
        val newIndex = (kfIndex + delta).coerceIn(0, trackEntity.keyframeEntities.size - 1)
        if (newIndex == kfIndex) return
        val kfe = trackEntity.keyframeEntities.removeAt(kfIndex)
        trackEntity.keyframeEntities.add(newIndex, kfe)
        player.setTag(ACTIVE_KEYFRAME_TAG, newIndex)
        refreshAllKeyframeNametags(trackEntity)
        if (kfIndex == 0 || newIndex == 0) trackEntity.repositionFromKeyframes()
        player.sendActionBar(Component.text("Keyframe moved to #${newIndex + 1}"))
    }

    private fun refreshAllKeyframeNametags(trackEntity: CameraTrackEntity) {
        trackEntity.keyframeEntities.forEach { it.updateNametag() }
        trackEntity.updateNametag()
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

    internal fun setActiveMarker(player: Player, name: String) {
        if (player.getTag(ACTIVE_MARKER_TAG) == name) return
        player.removeTag(ACTIVE_ANCHOR_TAG)
        player.removeTag(ACTIVE_REGION_TAG)
        player.removeTag(ACTIVE_CONNECTOR_TAG)
        player.setTag(ACTIVE_MARKER_TAG, name)
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1.0f, 1.0f))
    }

    private fun setActiveAnchor(player: Player, uuid: UUID, markerName: String) {
        if (player.getTag(ACTIVE_ANCHOR_TAG) == uuid) return
        player.removeTag(ACTIVE_REGION_TAG)
        player.removeTag(ACTIVE_CONNECTOR_TAG)
        player.setTag(ACTIVE_ANCHOR_TAG, uuid)
        player.setTag(ACTIVE_MARKER_TAG, markerName)
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1.0f, 1.0f))
    }

    private fun setActiveRegion(player: Player, regionId: String) {
        if (player.getTag(ACTIVE_REGION_TAG) == regionId) return
        player.removeTag(ACTIVE_MARKER_TAG)
        player.removeTag(ACTIVE_ANCHOR_TAG)
        player.removeTag(ACTIVE_CONNECTOR_TAG)
        player.setTag(ACTIVE_REGION_TAG, regionId)
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1.0f, 1.0f))
    }

    /**
     * Infers the most specific [PropertyValue] type from a string value.
     * Tries Boolean (strict), then Int, then Double, then falls back to String.
     */
    internal fun inferPropertyValue(value: String): PropertyValue = when {
        value.toBooleanStrictOrNull() != null -> PropertyValue.BoolValue(value.toBooleanStrict())
        value.toIntOrNull() != null -> PropertyValue.IntValue(value.toInt())
        value.toDoubleOrNull() != null -> PropertyValue.DoubleValue(value.toDouble())
        else -> PropertyValue.StringValue(value)
    }

    private fun drawTrackPaths() {
        trackEntities.values.forEach { trackEntity ->
            val kfes = trackEntity.keyframeEntities
            if (kfes.size < 2) return@forEach
            for (i in 0 until kfes.size - 1) {
                val from = kfes[i].position
                val to = kfes[i + 1].position
                val steps = 12
                for (step in 0..steps) {
                    val t = step.toDouble() / steps
                    val x = from.x + (to.x - from.x) * t
                    val y = from.y + (to.y - from.y) * t
                    val z = from.z + (to.z - from.z) * t
                    players.forEach { player ->
                        player.sendPacket(
                            net.minestom.server.network.packet.server.play.ParticlePacket(
                                net.minestom.server.particle.Particle.DUST.withProperties(
                                    net.minestom.server.color.DyeColor.PURPLE, 0.3f
                                ),
                                x, y, z, 0f, 0f, 0f, 0f, 1
                            )
                        )
                    }
                }
            }
        }
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
            val pos = player.position
            bounds.update(pos.blockX(), pos.blockY(), pos.blockZ())

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

            player.getTag(ACTIVE_WAYPOINT_TAG)?.let { name ->
                player.sendActionBar(Component.text("Waypoint: $name — \$waypoint save $name to overwrite"))
            }

            player.getTag(ACTIVE_TRACK_TAG)?.let { trackId ->
                val te = trackEntities[trackId]
                val kfIndex = player.getTag(ACTIVE_KEYFRAME_TAG)
                if (kfIndex != null && te != null) {
                    val kfe = te.keyframeEntities.getOrNull(kfIndex)
                    if (kfe != null) {
                        player.sendActionBar(Component.text(
                            "Keyframe #${kfIndex + 1} in '$trackId' — ${"%.1f".format(kfe.duration)}s | ${kfe.pathMode.name} | ${kfe.easing.name} — \$track duration/pathmode/easing/position/remove"
                        ))
                    }
                } else if (te != null) {
                    val totalDur = te.keyframeEntities.drop(1).sumOf { it.duration }
                    player.sendActionBar(Component.text(
                        "Track: $trackId (${te.keyframeEntities.size} kf, ${"%.1f".format(totalDur)}s) — \$track add / \$track remove"
                    ))
                }
            }

            player.getTag(ACTIVE_CONNECTOR_TAG)?.let { index ->
                val entity = connectorEntities.getOrNull(index)
                if (entity != null) {
                    val typeLabel = entity.type.name.lowercase()
                    val dirLabel = entity.direction.name.lowercase()
                    val lp = entity.localPosition
                    player.sendActionBar(Component.text(
                        "Connector #${index + 1}: $typeLabel facing $dirLabel at ${lp.x()},${lp.y()},${lp.z()} — \$connector remove / \$connector list"
                    ))
                }
            }

            player.getTag(ACTIVE_DECORATION_TAG)?.let { activeId ->
                val entity = decorationEntities.find { it.entityId == activeId }
                if (entity != null) {
                    val idx = decorationEntities.indexOf(entity) + 1
                    player.sendActionBar(Component.text(
                        "Decoration #$idx: ${entity.decorationType.asString()} — \$decoration prop/remove/list"
                    ))
                }
            }

            player.getTag(ACTIVE_BLOCK_TAG)?.let { posStr ->
                val parts = posStr.split(",")
                val bx = parts[0].toInt()
                val by = parts[1].toInt()
                val bz = parts[2].toInt()
                val worldPos = Vector3i(bx, by, bz)
                val localPos = "(${bx - ORIGIN.blockX()},${by - ORIGIN.blockY()},${bz - ORIGIN.blockZ()})"
                val data = blockDataMap[worldPos]
                val propsDisplay = if (data.isNullOrEmpty()) "no data"
                    else data.entries.joinToString(", ") { "${it.key}=${it.value.display}" }
                player.sendActionBar(Component.text("Block $localPos — $propsDisplay — \$block prop/clear/list"))
            }
        }

        drawTrackPaths()
    }

    override fun setBlock(x: Int, y: Int, z: Int, block: Block, doBlockUpdates: Boolean) {
        super.setBlock(x, y, z, block, doBlockUpdates)
        bounds.update(x, y, z)
    }

    override fun placeBlock(placement: BlockHandler.Placement, doBlockUpdates: Boolean): Boolean {
        if (super.placeBlock(placement, doBlockUpdates)) {
            val position = placement.blockPosition
            bounds.update(position.blockX(), position.blockY(), position.blockZ())
            return true
        }

        return false
    }

    override fun breakBlock(player: Player, position: Point, face: BlockFace, doBlockUpdates: Boolean): Boolean {
        if (super.breakBlock(player, position, face, doBlockUpdates)) {
            bounds.update(position.blockX(), position.blockY(), position.blockZ())
            return true
        }

        return false
    }

    fun save(folder: File) {
        val path = "${blueprintId.namespace()}/${blueprintId.value()}"
        val file = folder.resolve("$path.nbt")

        val root = bounds.min
        val blockMap = MinestomBlueprintHelper.getBlocks(this, bounds)
        val regions = regionHandler.collectRegions(root)
        val markerGroups = entities.filterIsInstance<MarkerGroupEntity>()
        val waypoints = waypointEntities.mapValues { (_, entity) -> entity.toWaypoint(root) }
        val cameraTracks = trackEntities.mapValues { (_, entity) ->
            CameraTrack(entity.keyframeEntities.map { it.toKeyframe(root) })
        }

        // Collect connectors from live entities — local positions are already stored correctly.
        val connectors = connectorEntities.map { entity ->
            val wp = entity.position
            val localPos = Vector3i(
                wp.blockX() - root.blockX(),
                wp.blockY() - root.blockY(),
                wp.blockZ() - root.blockZ(),
            )
            entity.connector.copy(position = localPos)
        }

        val decorations = decorationEntities.map { it.createDecoration(root) }

        val blockData = blockDataMap
            .filter { (_, props) -> props.isNotEmpty() }
            .map { (worldPos, props) ->
                BlockDatum(
                    position = Vector3i(
                        worldPos.x - root.blockX(),
                        worldPos.y - root.blockY(),
                        worldPos.z - root.blockZ(),
                    ),
                    properties = props.toMap(),
                )
            }

        val blueprint = MinestomBlueprintHelper.createBlueprint(root, blockMap, markerGroups, regions, connectors)
            .copy(waypoints = waypoints, cameraTracks = cameraTracks, decorations = decorations, blockData = blockData)

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
        val ACTIVE_WAYPOINT_TAG: Tag<String> = Tag.String("active_waypoint")
        val ACTIVE_TRACK_TAG: Tag<String> = Tag.String("active_track")
        val ACTIVE_KEYFRAME_TAG: Tag<Int> = Tag.Integer("active_keyframe")
        val ACTIVE_CONNECTOR_TAG: Tag<Int> = Tag.Integer("active_connector")
        val ACTIVE_DECORATION_TAG: Tag<Int> = Tag.Integer("active_decoration")
        val ACTIVE_BLOCK_TAG: Tag<String> = Tag.String("active_block")
    }
}
