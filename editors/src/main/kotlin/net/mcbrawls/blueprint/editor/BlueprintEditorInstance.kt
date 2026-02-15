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
import net.mcbrawls.blueprint.editor.anchor.AnchorEntity
import net.mcbrawls.blueprint.editor.anchor.AnchorModType
import net.mcbrawls.blueprint.editor.anchor.DecorationAnchorEntity
import net.mcbrawls.blueprint.editor.region.InstanceRegionHandler
import net.mcbrawls.blueprint.minestom.MinestomBlueprintSerializer
import net.mcbrawls.blueprint.minestom.MinestomBlueprints.combinedPos
import net.mcbrawls.blueprint.util.NbtOps
import net.mcbrawls.codex.encodeQuick
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
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
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.world.DimensionType
import org.joml.Vector2f
import org.joml.Vector3d
import java.io.File
import java.util.Optional
import java.util.UUID

class BlueprintEditorInstance(val blueprintId: Key, val blueprint: Blueprint<Block>?) : InstanceContainer(UUID.randomUUID(), DimensionType.OVERWORLD) {
    private var initialized: Boolean = false

    private var placedBlueprint: PlacedBlueprint<Block>? = null

    private val bounds = Bounds(ORIGIN)

    private val regionHandler = InstanceRegionHandler(this, ORIGIN, blueprint?.regions ?: emptyMap())

    fun initializeInternal() {
        if (blueprint != null) {
            val placed = MinestomBlueprintSerializer.placeBlueprint(this, ORIGIN, blueprint)
            placedBlueprint = placed

            placed.getAllAnchors().forEach { (id, anchor) ->
                spawnAnchor(id, anchor)
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

            player.removeTag(ACTIVE_ANCHOR_TAG)
            player.removeTag(ANCHOR_MOD_TYPE_TAG)
            player.removeTag(ACTIVE_REGION_TAG)
        }

        node.addListener(PlayerChatEvent::class.java) { event ->
            event.isCancelled = true

            val player = event.player
            val str = event.rawMessage

            if (regionHandler.hasActiveCreationSession(player)) {
                if (str.startsWith("$")) {
                    // Check for region-specific commands
                    when (str) {
                        $$"$region cancel" -> {
                            regionHandler.cancelCreation(player)
                        }
                    }
                } else {
                    // Treat as region ID
                    if (regionHandler.confirmRegion(player, str)) {
                        regionHandler.exitCreationMode(player)
                    }
                }

                return@addListener
            }

            var shouldReturn = true
            when (str) {
                $$"$clear" -> {
                    player.removeTag(ACTIVE_ANCHOR_TAG)
                    player.removeTag(ANCHOR_MOD_TYPE_TAG)
                    player.removeTag(ACTIVE_REGION_TAG)
                    player.sendActionBar(Component.text("Cleared active selection"))
                }

                $$"$teleport" -> {
                    player.getTag(ACTIVE_ANCHOR_TAG)?.let { uuid ->
                        (getEntityByUuid(uuid) as? AnchorEntity)?.let { entity ->
                            player.teleport(entity.position.sub(0.0, player.eyeHeight, 0.0).withView(player.position))
                        }
                    }
                }

                $$"$removedata" -> {
                    player.getTag(ACTIVE_ANCHOR_TAG)?.let { uuid ->
                        (getEntityByUuid(uuid) as? AnchorEntity)?.let { entity ->
                            entity.anchorData = null
                            entity.updateNametag()
                        }
                    }
                }

                $$"$remove" -> {
                    player.getTag(ACTIVE_ANCHOR_TAG)?.let { uuid ->
                        getEntityByUuid(uuid)?.let { entity ->
                            entity.remove()
                            player.sendActionBar(Component.text("Removed anchor"))
                        }
                    }

                    player.getTag(ACTIVE_REGION_TAG)?.let { regionId ->
                        regionHandler.deleteRegion(regionId)
                        player.removeTag(ACTIVE_REGION_TAG)
                        player.sendActionBar(Component.text("Deleted region '$regionId'", NamedTextColor.RED))
                    }
                }

                $$"$region create" -> {
                    regionHandler.enterCreationMode(player)
                    shouldReturn = true
                }

                $$"$region exit" -> {
                    regionHandler.exitCreationMode(player)
                    shouldReturn = true
                }

                $$"$region toggle" -> {
                    val enabled = regionHandler.togglePlayerParticleVisualization(player)
                    val status = if (enabled) "enabled" else "disabled"
                    player.sendActionBar(Component.text("Region particles $status"))
                    shouldReturn = true
                }

                else -> shouldReturn = false
            }

            if (shouldReturn) return@addListener

            // Handle region ID modification
            player.getTag(ACTIVE_REGION_TAG)?.let { regionId ->
                regionHandler.renameRegion(regionId, str)
                player.setTag(ACTIVE_REGION_TAG, str)
                player.sendActionBar(Component.text("Renamed region to '$str'"))
                return@addListener
            }

            player.getTag(ACTIVE_ANCHOR_TAG)?.let { uuid ->
                player.getTag(ANCHOR_MOD_TYPE_TAG)?.let { modType ->
                    (getEntityByUuid(uuid) as? AnchorEntity)?.let { entity ->
                        when (modType) {
                            AnchorModType.ID -> entity.anchorId = str
                            AnchorModType.DATA -> entity.anchorData = str
                        }

                        entity.updateNametag()
                    }
                }
            }
        }

        node.addListener(PlayerEntityInteractEvent::class.java) { event ->
            val player = event.player
            val entity = event.target

            // Handle region selection
            if (entity is InstanceRegionHandler.RegionEntity) {
                setActiveRegion(player, entity.regionId)
                return@addListener
            }

            // Handle anchor selection
            (entity as? AnchorEntity)?.let { anchorEntity ->
                val uuid = anchorEntity.uuid
                setActiveAnchor(player, uuid, AnchorModType.ID)
            }
        }

        node.addListener(EntityAttackEvent::class.java) { event ->
            val player = event.entity as? Player ?: return@addListener
            val entity = event.target as? AnchorEntity ?: return@addListener
            val uuid = entity.uuid
            setActiveAnchor(player, uuid, AnchorModType.DATA)
        }

        node.addListener(PlayerUseItemOnBlockEvent::class.java) { event ->
            if (event.hand == PlayerHand.OFF) return@addListener

            val player = event.player
            val point = event.position.add(event.cursorPosition)
            val playerPosition = player.position
            val position = Pos(point, playerPosition.yaw, playerPosition.pitch)

            if (regionHandler.hasActiveCreationSession(player)) {
                val regionPos = Vector3d(point.x(), point.y(), point.z())
                regionHandler.setPosition(player, regionPos)
                return@addListener
            }

            val anchorPos = Vector3d(position.x, position.y, position.z)
            val anchorRot = Vector2f(position.yaw, position.pitch)

            // TODO decorations placement
            when (event.itemStack.material()) {
                Material.STICK -> spawnAnchor("decoration", Anchor(anchorPos, anchorRot, Optional.of("fracture:jump_pad")))
                Material.WOODEN_HOE -> {
                    val entity = spawnAnchor(UUID.randomUUID().toString(), Anchor(anchorPos, anchorRot, Optional.empty()))
                    setActiveAnchor(player, entity.uuid, AnchorModType.ID)
                }
                else -> {}
            }
        }

        node.addListener(PlayerPickBlockEvent::class.java) { event ->
            runCatching {
                val player = event.player
                val heldSlot = player.heldSlot
                val inventory = player.inventory

                val key = event.block.key()
                val material = Material.fromKey(key)
                val stack = ItemStack.builder(material).build()

                for (i in 0 until 9) {
                    val hotbarStack = inventory.getItemStack(i)
                    if (hotbarStack.isSimilar(stack)) {
                        player.setHeldItemSlot(i.toByte())
                        return@addListener
                    }
                }

                val existingStack = inventory.getItemStack(heldSlot.toInt())

                if (!existingStack.isSimilar(stack)) {
                    inventory.setItemStack(heldSlot.toInt(), stack)

                    if (!existingStack.isAir) {
                        inventory.addItemStack(existingStack)
                    }
                }
            }
        }
    }

    private fun spawnAnchor(id: String, anchor: Anchor): Entity {
        return when (id) {
            "decoration" -> {
                try {
                    val data = anchor.data.orElse("")
                    val key = Key.key(data)
                    val entity = DecorationAnchorEntity(key)
                    entity.setInstance(this, anchor.combinedPos)
                    entity
                } catch (_: Throwable) {
                    spawnDefaultAnchor(id, anchor)
                }
            }

            else -> spawnDefaultAnchor(id, anchor)
        }
    }

    private fun spawnDefaultAnchor(id: String, anchor: Anchor): AnchorEntity {
        val entity = AnchorEntity(id, anchor)
        entity.setInstance(this, anchor.combinedPos)
        return entity
    }

    private fun setActiveAnchor(player: Player, uuid: UUID, type: AnchorModType) {
        if (player.getTag(ANCHOR_MOD_TYPE_TAG) == type && player.getTag(ACTIVE_ANCHOR_TAG) == uuid) return

        player.removeTag(ACTIVE_REGION_TAG)
        player.setTag(ACTIVE_ANCHOR_TAG, uuid)
        player.setTag(ANCHOR_MOD_TYPE_TAG, type)
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1.0f, 1.0f))
    }

    private fun setActiveRegion(player: Player, regionId: String) {
        if (player.getTag(ACTIVE_REGION_TAG) == regionId) return

        player.removeTag(ACTIVE_ANCHOR_TAG)
        player.removeTag(ANCHOR_MOD_TYPE_TAG)
        player.setTag(ACTIVE_REGION_TAG, regionId)
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1.0f, 1.0f))
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
            player.getTag(ANCHOR_MOD_TYPE_TAG)?.let { tag ->
                player.sendActionBar(Component.text("Modifying anchor: $tag"))
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
        // prepare file
        val path = "${blueprintId.namespace()}/${blueprintId.value()}"
        val file = folder.resolve("$path.nbt")

        // create blueprint
        val root = bounds.min
        val blockMap = MinestomBlueprintHelper.getBlocks(this, bounds)
        val regions = regionHandler.collectRegions(root)
        val anchors = entities.filterIsInstance<AnchorEntity>()
        val blueprint = MinestomBlueprintHelper.createBlueprint(root, blockMap, anchors, regions)

        // serialize
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

        val ACTIVE_ANCHOR_TAG: Tag<UUID> = Tag.UUID("active_anchor")
        val ANCHOR_MOD_TYPE_TAG: Tag<AnchorModType> = Tag.Transient("anchor_mod_type")
        val ACTIVE_REGION_TAG: Tag<String> = Tag.String("active_region")
    }
}
