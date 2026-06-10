package net.mcbrawls.blueprint.editor

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.mcbrawls.blueprint.CardinalDirection
import net.mcbrawls.blueprint.ConnectorType
import net.mcbrawls.blueprint.camera.EasingFunction
import net.mcbrawls.blueprint.camera.PathMode
import net.mcbrawls.blueprint.editor.anchor.MarkerAnchorEntity
import net.mcbrawls.blueprint.editor.anchor.MarkerGroupEntity
import net.mcbrawls.blueprint.editor.waypoint.WaypointEntity
import net.mcbrawls.blueprint.minestom.MinestomBlueprintSerializer
import net.mcbrawls.fracture.command.AbstractCommand
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.arguments.ArgumentType.Literal
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.sound.SoundEvent
import org.joml.Vector3i

class BpeCommand(
    private val serializer: MinestomBlueprintSerializer,
    name: String,
    vararg aliases: String,
) : AbstractCommand(name, *aliases) {

    private val blueprintArg = ArgumentType.String("blueprint").apply {
        setSuggestionCallback { _, _, suggestion ->
            serializer.collectBlueprints().keys.forEach { suggestion.addEntry(SuggestionEntry(it)) }
        }
    }
    private val markerNameArg = ArgumentType.Word("marker_name").apply {
        setSuggestionCallback { sender, _, suggestion ->
            val player = sender as? Player ?: return@setSuggestionCallback
            val instance = player.instance as? BlueprintEditorInstance ?: return@setSuggestionCallback
            instance.markerEntities.keys.forEach { suggestion.addEntry(SuggestionEntry(it)) }
        }
    }
    private val waypointNameArg = ArgumentType.Word("waypoint_name").apply {
        setSuggestionCallback { sender, _, suggestion ->
            val player = sender as? Player ?: return@setSuggestionCallback
            val instance = player.instance as? BlueprintEditorInstance ?: return@setSuggestionCallback
            instance.waypointEntities.keys.forEach { suggestion.addEntry(SuggestionEntry(it)) }
        }
    }
    private val trackIdArg = ArgumentType.Word("track_id").apply {
        setSuggestionCallback { sender, _, suggestion ->
            val player = sender as? Player ?: return@setSuggestionCallback
            val instance = player.instance as? BlueprintEditorInstance ?: return@setSuggestionCallback
            instance.trackEntities.keys.forEach { suggestion.addEntry(SuggestionEntry(it)) }
        }
    }
    private val typeKeyArg   = ArgumentType.String("type_key")
    private val propKeyArg   = ArgumentType.Word("prop_key")
    private val propValueArg = ArgumentType.Word("prop_value")
    private val secondsArg   = ArgumentType.Float("seconds")
    private val moveIndexArg = ArgumentType.Integer("index")
    private val pathModeArg  = ArgumentType.Word("path_mode").apply {
        setSuggestionCallback { _, _, suggestion ->
            PathMode.entries.forEach { suggestion.addEntry(SuggestionEntry(it.name.lowercase())) }
        }
    }
    private val easingArg = ArgumentType.Word("easing").apply {
        setSuggestionCallback { _, _, suggestion ->
            EasingFunction.entries.forEach { suggestion.addEntry(SuggestionEntry(it.name.lowercase())) }
        }
    }
    private val directionArg = ArgumentType.Word("direction").apply {
        setSuggestionCallback { _, _, suggestion ->
            CardinalDirection.entries.forEach { suggestion.addEntry(SuggestionEntry(it.name.lowercase())) }
        }
    }
    private val connectorTypeArg = ArgumentType.Word("connector_type").apply {
        setSuggestionCallback { _, _, suggestion ->
            ConnectorType.entries.forEach { suggestion.addEntry(SuggestionEntry(it.name.lowercase())) }
        }
    }
    private val decorTypeArg = ArgumentType.String("decor_type")

    init {
        // /bpe open <blueprint>
        addSyntax {
            requireBase()
            args(ArgumentType.Literal("open"), blueprintArg)
            playerExecutor { player, context ->
                val blueprintId = Key.key(context[blueprintArg])
                val blueprint = serializer[blueprintId]
                BlueprintEditorHandler.add(blueprint, blueprintId) { instance ->
                    val rootPos = BlueprintEditorInstance.ORIGIN.asPos()
                    val pos = blueprint?.size?.let { size ->
                        rootPos.add(size.x() / 2.0, size.y() / 2.0, size.z() / 2.0)
                    } ?: rootPos
                    player.setInstance(instance, pos).join()
                    player.gameMode = GameMode.SPECTATOR
                }
                player.sendMessage("Opening blueprint editor: $blueprintId")
            }
        }

        // /bpe save
        addSyntax {
            requireBase()
            args(ArgumentType.Literal("save"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                BlueprintEditorHandler.save(instance)
                player.sendMessage(Component.text("Saved blueprint: ${instance.blueprintId}"))
            }
        }

        // /bpe save <blueprint>
        addSyntax {
            requireBase()
            args(ArgumentType.Literal("save"), blueprintArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.blueprintId = Key.key(context[blueprintArg])
                BlueprintEditorHandler.save(instance)
                player.sendMessage(Component.text("Saved blueprint: ${instance.blueprintId}"))
            }
        }

        // /bpe clear
        addSyntax {
            requireBase()
            args(Literal("clear"))
            playerExecutor { player, _ ->
                player.removeTag(BlueprintEditorInstance.ACTIVE_MARKER_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_ANCHOR_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_REGION_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_WAYPOINT_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_TRACK_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_KEYFRAME_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_CONNECTOR_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_DECORATION_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_BLOCK_TAG)
                player.sendActionBar(Component.text("Cleared selection"))
            }
        }

        // /bpe marker <name>  - create with default type
        addSyntax {
            requireBase()
            args(Literal("marker"), markerNameArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val name = context[markerNameArg]
                if (instance.markerEntities.containsKey(name)) {
                    player.sendActionBar(Component.text("Marker '$name' already exists", NamedTextColor.RED))
                    return@playerExecutor
                }
                val entity = MarkerGroupEntity(name, Key.key("blueprint", "unknown"))
                entity.setInstance(instance, player.position)
                instance.markerEntities[name] = entity
                instance.setActiveMarker(player, name)
                player.sendActionBar(Component.text("Created marker '$name'"))
            }
        }

        // /bpe marker <name> <type_key>  - create with explicit type
        addSyntax {
            requireBase()
            args(Literal("marker"), markerNameArg, typeKeyArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val name = context[markerNameArg]
                if (instance.markerEntities.containsKey(name)) {
                    player.sendActionBar(Component.text("Marker '$name' already exists", NamedTextColor.RED))
                    return@playerExecutor
                }
                val type = runCatching { Key.key(context[typeKeyArg]) }.getOrElse {
                    player.sendActionBar(Component.text("Invalid key: ${context[typeKeyArg]}", NamedTextColor.RED))
                    return@playerExecutor
                }
                val entity = MarkerGroupEntity(name, type)
                entity.setInstance(instance, player.position)
                instance.markerEntities[name] = entity
                instance.setActiveMarker(player, name)
                player.sendActionBar(Component.text("Created marker '$name' ($type)"))
            }
        }

        // /bpe marker type <type_key>  - set type of active marker
        addSyntax {
            requireBase()
            args(Literal("marker"), Literal("type"), typeKeyArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val activeMarkerName = player.getTag(BlueprintEditorInstance.ACTIVE_MARKER_TAG) ?: run {
                    player.sendActionBar(Component.text("No marker selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                val newType = runCatching { Key.key(context[typeKeyArg]) }.getOrElse {
                    player.sendActionBar(Component.text("Invalid key: ${context[typeKeyArg]}", NamedTextColor.RED))
                    return@playerExecutor
                }
                instance.markerEntities[activeMarkerName]?.let { entity ->
                    entity.markerType = newType
                    entity.updateNametag()
                    player.sendActionBar(Component.text("Type set to $newType"))
                }
            }
        }

        // /bpe marker prop <key>  - remove property from active marker
        addSyntax {
            requireBase()
            args(Literal("marker"), Literal("prop"), propKeyArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val activeMarkerName = player.getTag(BlueprintEditorInstance.ACTIVE_MARKER_TAG) ?: run {
                    player.sendActionBar(Component.text("No marker selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                val key = context[propKeyArg]
                val entity = instance.markerEntities[activeMarkerName] ?: return@playerExecutor
                val updated = entity.properties.toMutableMap()
                updated.remove(key)
                entity.properties = updated
                entity.updateNametag()
                player.sendActionBar(Component.text("Removed property '$key'"))
            }
        }

        // /bpe marker prop <key> <value>  - set property on active marker
        addSyntax {
            requireBase()
            args(Literal("marker"), Literal("prop"), propKeyArg, propValueArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val activeMarkerName = player.getTag(BlueprintEditorInstance.ACTIVE_MARKER_TAG) ?: run {
                    player.sendActionBar(Component.text("No marker selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                val key = context[propKeyArg]
                val value = context[propValueArg]
                val entity = instance.markerEntities[activeMarkerName] ?: return@playerExecutor
                val updated = entity.properties.toMutableMap()
                updated[key] = instance.inferPropertyValue(value)
                entity.properties = updated
                entity.updateNametag()
                player.sendActionBar(Component.text("Set $key = $value"))
            }
        }

        // /bpe marker remove  - remove active marker
        addSyntax {
            requireBase()
            args(Literal("marker"), Literal("remove"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val activeMarkerName = player.getTag(BlueprintEditorInstance.ACTIVE_MARKER_TAG) ?: run {
                    player.sendActionBar(Component.text("No marker selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                instance.removeMarker(player, activeMarkerName)
            }
        }

        // /bpe anchor prop <key>  - remove property from active anchor
        addSyntax {
            requireBase()
            args(Literal("anchor"), Literal("prop"), propKeyArg)
            playerExecutor { player, context ->
                requireEditor(player) ?: return@playerExecutor
                val anchorUuid = player.getTag(BlueprintEditorInstance.ACTIVE_ANCHOR_TAG) ?: run {
                    player.sendActionBar(Component.text("No anchor selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                val anchor = player.instance.entities.filterIsInstance<MarkerAnchorEntity>()
                    .find { it.uuid == anchorUuid } ?: return@playerExecutor
                val key = context[propKeyArg]
                val updated = anchor.properties.toMutableMap()
                updated.remove(key)
                anchor.properties = updated
                anchor.updateNametag()
                player.sendActionBar(Component.text("Anchor: removed property '$key'"))
            }
        }

        // /bpe anchor prop <key> <value>  - set property on active anchor
        addSyntax {
            requireBase()
            args(Literal("anchor"), Literal("prop"), propKeyArg, propValueArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val anchorUuid = player.getTag(BlueprintEditorInstance.ACTIVE_ANCHOR_TAG) ?: run {
                    player.sendActionBar(Component.text("No anchor selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                val anchor = player.instance.entities.filterIsInstance<MarkerAnchorEntity>()
                    .find { it.uuid == anchorUuid } ?: return@playerExecutor
                val key = context[propKeyArg]
                val value = context[propValueArg]
                val updated = anchor.properties.toMutableMap()
                updated[key] = instance.inferPropertyValue(value)
                anchor.properties = updated
                anchor.updateNametag()
                player.sendActionBar(Component.text("Anchor: set $key = $value"))
            }
        }

        // /bpe anchor remove  - remove active anchor
        addSyntax {
            requireBase()
            args(Literal("anchor"), Literal("remove"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val anchorUuid = player.getTag(BlueprintEditorInstance.ACTIVE_ANCHOR_TAG) ?: run {
                    player.sendActionBar(Component.text("No anchor selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                val anchor = instance.entities.filterIsInstance<MarkerAnchorEntity>()
                    .find { it.uuid == anchorUuid } ?: return@playerExecutor
                instance.removeAnchor(player, anchor)
            }
        }

        // /bpe anchor teleport  - teleport to active anchor
        addSyntax {
            requireBase()
            args(Literal("anchor"), Literal("teleport"))
            playerExecutor { player, _ ->
                requireEditor(player) ?: return@playerExecutor
                val anchorUuid = player.getTag(BlueprintEditorInstance.ACTIVE_ANCHOR_TAG) ?: run {
                    player.sendActionBar(Component.text("No anchor selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                val anchor = player.instance.entities.filterIsInstance<MarkerAnchorEntity>()
                    .find { it.uuid == anchorUuid } ?: return@playerExecutor
                player.teleport(anchor.position.sub(0.0, player.eyeHeight, 0.0).withView(player.position))
            }
        }

        // /bpe region create
        addSyntax {
            requireBase()
            args(Literal("region"), Literal("create"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.regionHandler.enterCreationMode(player)
            }
        }

        // /bpe region exit
        addSyntax {
            requireBase()
            args(Literal("region"), Literal("exit"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.regionHandler.exitCreationMode(player)
            }
        }

        // /bpe region toggle
        addSyntax {
            requireBase()
            args(Literal("region"), Literal("toggle"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val enabled = instance.regionHandler.togglePlayerParticleVisualization(player)
                player.sendActionBar(Component.text("Region particles ${if (enabled) "enabled" else "disabled"}"))
            }
        }

        // /bpe region cancel
        addSyntax {
            requireBase()
            args(Literal("region"), Literal("cancel"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.regionHandler.cancelCreation(player)
            }
        }

        // /bpe region remove
        addSyntax {
            requireBase()
            args(Literal("region"), Literal("remove"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val regionId = player.getTag(BlueprintEditorInstance.ACTIVE_REGION_TAG) ?: run {
                    player.sendActionBar(Component.text("No region selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                instance.regionHandler.deleteRegion(regionId)
                player.removeTag(BlueprintEditorInstance.ACTIVE_REGION_TAG)
                player.sendActionBar(Component.text("Deleted region '$regionId'", NamedTextColor.RED))
            }
        }

        // /bpe waypoint <name>  - teleport to waypoint
        addSyntax {
            requireBase()
            args(Literal("waypoint"), waypointNameArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val name = context[waypointNameArg]
                val entity = instance.waypointEntities[name] ?: run {
                    player.sendActionBar(Component.text("Unknown waypoint '$name'", NamedTextColor.RED))
                    return@playerExecutor
                }
                entity.teleportPlayer(player)
                player.removeTag(BlueprintEditorInstance.ACTIVE_MARKER_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_ANCHOR_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_REGION_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_TRACK_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_KEYFRAME_TAG)
                player.removeTag(BlueprintEditorInstance.ACTIVE_CONNECTOR_TAG)
                player.setTag(BlueprintEditorInstance.ACTIVE_WAYPOINT_TAG, name)
                player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1f, 1f))
                player.sendActionBar(Component.text("Teleported to '$name'"))
            }
        }

        // /bpe waypoint save <name>  - save waypoint at current position
        addSyntax {
            requireBase()
            args(Literal("waypoint"), Literal("save"), waypointNameArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val name = context[waypointNameArg]
                instance.waypointEntities[name]?.remove()
                val entity = WaypointEntity(name)
                entity.setInstance(instance, player.position)
                instance.waypointEntities[name] = entity
                player.setTag(BlueprintEditorInstance.ACTIVE_WAYPOINT_TAG, name)
                player.sendActionBar(Component.text("Saved waypoint '$name'"))
            }
        }

        // /bpe waypoint remove  - remove active waypoint
        addSyntax {
            requireBase()
            args(Literal("waypoint"), Literal("remove"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val name = player.getTag(BlueprintEditorInstance.ACTIVE_WAYPOINT_TAG) ?: run {
                    player.sendActionBar(Component.text("No waypoint selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                instance.waypointEntities.remove(name)?.remove()
                player.removeTag(BlueprintEditorInstance.ACTIVE_WAYPOINT_TAG)
                player.sendActionBar(Component.text("Removed waypoint '$name'", NamedTextColor.RED))
            }
        }

        // /bpe track <id>  - select or create track (fallback, literals take priority)
        addSyntax {
            requireBase()
            args(Literal("track"), trackIdArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", context[trackIdArg]))
            }
        }

        // /bpe track add
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("add"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "add"))
            }
        }

        // /bpe track remove
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("remove"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "remove"))
            }
        }

        // /bpe track duration <seconds>
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("duration"), secondsArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "duration", context[secondsArg].toString()))
            }
        }

        // /bpe track pathmode <mode>
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("pathmode"), pathModeArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "pathmode", context[pathModeArg]))
            }
        }

        // /bpe track easing <function>
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("easing"), easingArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "easing", context[easingArg]))
            }
        }

        // /bpe track teleport
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("teleport"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "teleport"))
            }
        }

        // /bpe track position
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("position"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "position"))
            }
        }

        // /bpe track up
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("up"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "up"))
            }
        }

        // /bpe track down
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("down"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "down"))
            }
        }

        // /bpe track move <index>  - 1-based index, matches original chat command behaviour
        addSyntax {
            requireBase()
            args(Literal("track"), Literal("move"), moveIndexArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleTrackCommand(player, listOf("track", "move", context[moveIndexArg].toString()))
            }
        }

        // /bpe connector add <direction> <type>
        addSyntax {
            requireBase()
            args(Literal("connector"), Literal("add"), directionArg, connectorTypeArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleConnectorCommand(
                    player,
                    listOf("connector", "add", context[directionArg], context[connectorTypeArg])
                )
            }
        }

        // /bpe connector remove
        addSyntax {
            requireBase()
            args(Literal("connector"), Literal("remove"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleConnectorCommand(player, listOf("connector", "remove"))
            }
        }

        // /bpe connector list
        addSyntax {
            requireBase()
            args(Literal("connector"), Literal("list"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleConnectorCommand(player, listOf("connector", "list"))
            }
        }

        // /bpe decoration add <type_key>
        addSyntax {
            requireBase()
            args(Literal("decoration"), Literal("add"), decorTypeArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleDecorationCommand(player, listOf("decoration", "add", context[decorTypeArg]))
            }
        }

        // /bpe decoration remove
        addSyntax {
            requireBase()
            args(Literal("decoration"), Literal("remove"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleDecorationCommand(player, listOf("decoration", "remove"))
            }
        }

        // /bpe decoration prop <key>  - remove property from active decoration
        addSyntax {
            requireBase()
            args(Literal("decoration"), Literal("prop"), propKeyArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleDecorationCommand(player, listOf("decoration", "prop", context[propKeyArg]))
            }
        }

        // /bpe decoration prop <key> <value>  - set property on active decoration
        addSyntax {
            requireBase()
            args(Literal("decoration"), Literal("prop"), propKeyArg, propValueArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleDecorationCommand(
                    player,
                    listOf("decoration", "prop", context[propKeyArg], context[propValueArg])
                )
            }
        }

        // /bpe decoration list
        addSyntax {
            requireBase()
            args(Literal("decoration"), Literal("list"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                instance.handleDecorationCommand(player, listOf("decoration", "list"))
            }
        }

        // /bpe block prop <key>  - remove property from selected block
        addSyntax {
            requireBase()
            args(Literal("block"), Literal("prop"), propKeyArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val posStr = player.getTag(BlueprintEditorInstance.ACTIVE_BLOCK_TAG) ?: run {
                    player.sendActionBar(Component.text("No block selected — right-click with feather to select", NamedTextColor.RED))
                    return@playerExecutor
                }
                val (bx, by, bz) = posStr.split(",").map { it.toInt() }
                val key = context[propKeyArg]
                instance.blockDataMap.getOrPut(Vector3i(bx, by, bz)) { mutableMapOf() }.remove(key)
                player.sendActionBar(Component.text("Block: removed property '$key'"))
            }
        }

        // /bpe block prop <key> <value>  - set property on selected block
        addSyntax {
            requireBase()
            args(Literal("block"), Literal("prop"), propKeyArg, propValueArg)
            playerExecutor { player, context ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val posStr = player.getTag(BlueprintEditorInstance.ACTIVE_BLOCK_TAG) ?: run {
                    player.sendActionBar(Component.text("No block selected — right-click with feather to select", NamedTextColor.RED))
                    return@playerExecutor
                }
                val (bx, by, bz) = posStr.split(",").map { it.toInt() }
                val key = context[propKeyArg]
                val value = context[propValueArg]
                instance.blockDataMap.getOrPut(Vector3i(bx, by, bz)) { mutableMapOf() }[key] = instance.inferPropertyValue(value)
                player.sendActionBar(Component.text("Block: set $key = $value"))
            }
        }

        // /bpe block list  - list all properties of selected block
        addSyntax {
            requireBase()
            args(Literal("block"), Literal("list"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val posStr = player.getTag(BlueprintEditorInstance.ACTIVE_BLOCK_TAG) ?: run {
                    player.sendActionBar(Component.text("No block selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                val (bx, by, bz) = posStr.split(",").map { it.toInt() }
                val data = instance.blockDataMap[Vector3i(bx, by, bz)]
                if (data.isNullOrEmpty()) {
                    player.sendActionBar(Component.text("Block has no data"))
                } else {
                    data.entries.forEach { (k, v) ->
                        player.sendMessage(Component.text("$k = ${v.display}", NamedTextColor.AQUA))
                    }
                }
            }
        }

        // /bpe block clear  - clear all properties from selected block
        addSyntax {
            requireBase()
            args(Literal("block"), Literal("clear"))
            playerExecutor { player, _ ->
                val instance = requireEditor(player) ?: return@playerExecutor
                val posStr = player.getTag(BlueprintEditorInstance.ACTIVE_BLOCK_TAG) ?: run {
                    player.sendActionBar(Component.text("No block selected", NamedTextColor.RED))
                    return@playerExecutor
                }
                val (bx, by, bz) = posStr.split(",").map { it.toInt() }
                instance.blockDataMap.remove(Vector3i(bx, by, bz))
                player.sendActionBar(Component.text("Block: cleared all properties", NamedTextColor.RED))
            }
        }
    }

    private fun requireEditor(player: Player): BlueprintEditorInstance? {
        val instance = player.instance as? BlueprintEditorInstance
        if (instance == null) player.sendActionBar(Component.text("Not in a blueprint editor", NamedTextColor.RED))
        return instance
    }
}
