package net.mcbrawls.fracture.blueprint.editor

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.mcbrawls.blueprint.Anchor
import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.PlacedBlueprint
import net.mcbrawls.blueprint.Vec2f
import net.mcbrawls.blueprint.Vec3d
import net.mcbrawls.blueprint.minestom.MinestomBlueprintSerializer
import net.mcbrawls.blueprint.minestom.MinestomBlueprints.combinedPos
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
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
import java.util.Optional
import java.util.UUID

class BlueprintEditorInstance(val blueprintId: Key, val blueprint: Blueprint<Block>) : InstanceContainer(UUID.randomUUID(), DimensionType.OVERWORLD) {
    private var initialized: Boolean = false

    private lateinit var placedBlueprint: PlacedBlueprint<Block>

    fun initializeInternal() {
        placedBlueprint = MinestomBlueprintSerializer.placeBlueprint(this, ORIGIN, blueprint)

        placedBlueprint.getAllAnchors().forEach { (id, anchor) ->
            spawnAnchor(id, anchor)
        }
    }

    fun initializeEvents(node: EventNode<InstanceEvent>) {
        node.addListener(RemoveEntityFromInstanceEvent::class.java) { event ->
            val player = event.entity as? Player ?: return@addListener
            player.removeTag(ACTIVE_ANCHOR_TAG)
            player.removeTag(ANCHOR_MOD_TYPE_TAG)
        }

        node.addListener(PlayerChatEvent::class.java) { event ->
            event.isCancelled = true

            val player = event.player
            val str = event.rawMessage

            var shouldReturn = true
            when (str) {
                $$"$clear" -> {
                    player.removeTag(ACTIVE_ANCHOR_TAG)
                    player.removeTag(ANCHOR_MOD_TYPE_TAG)
                    player.sendActionBar(Component.text("Cleared active anchor mod"))
                }

                $$"$teleport" -> {
                    player.getTag(ACTIVE_ANCHOR_TAG)?.let { uuid ->
                        (getEntityByUuid(uuid) as? AnchorEntity)?.let { entity ->
                            player.teleport(entity.position.sub(0.0, player.eyeHeight, 0.0).withView(player.position))
                        }
                    }
                }

                else -> shouldReturn = false
            }

            if (shouldReturn) return@addListener

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
            val entity = event.target as? AnchorEntity ?: return@addListener
            val uuid = entity.uuid
            setActiveAnchor(player, uuid, AnchorModType.ID)
        }

        node.addListener(EntityAttackEvent::class.java) { event ->
            val player = event.entity as? Player ?: return@addListener
            val entity = event.target as? AnchorEntity ?: return@addListener
            val uuid = entity.uuid
            setActiveAnchor(player, uuid, AnchorModType.DATA)
        }

        // TODO decorations placement
        node.addListener(PlayerUseItemOnBlockEvent::class.java) { event ->
            if (event.itemStack.material() == Material.STICK) {
                val player = event.player
                val point = event.position.add(event.cursorPosition)
                val playerPosition = player.position
                val position = Pos(point, playerPosition.yaw, playerPosition.pitch)

                val anchorPos = Vec3d(position.x, position.y, position.z)
                val anchorRot = Vec2f(position.yaw, position.pitch)

                spawnAnchor("decoration", Anchor(anchorPos, anchorRot, Optional.of("fracture:jump_pad")))
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

    private fun spawnAnchor(id: String, anchor: Anchor) {
        when (id) {
            "decoration" -> {
                runCatching {
                    val data = anchor.data.orElse("")
                    val key = Key.key(data)
                    val entity = DecorationAnchorEntity(key)
                    entity.setInstance(this, anchor.combinedPos)
                }.onFailure {
                    spawnDefaultAnchor(id, anchor)
                }
            }

            else -> {
                spawnDefaultAnchor(id, anchor)
            }
        }
    }

    private fun spawnDefaultAnchor(id: String, anchor: Anchor) {
        val entity = AnchorEntity(id, anchor)
        entity.setInstance(this, anchor.combinedPos)
    }

    private fun setActiveAnchor(player: Player, uuid: UUID, type: AnchorModType) {
        if (player.getTag(ANCHOR_MOD_TYPE_TAG) == type && player.getTag(ACTIVE_ANCHOR_TAG) == uuid) return

        player.setTag(ACTIVE_ANCHOR_TAG, uuid)
        player.setTag(ANCHOR_MOD_TYPE_TAG, type)
        player.playSound(Sound.sound(SoundEvent.UI_BUTTON_CLICK.key(), Sound.Source.PLAYER, 1.0f, 1.0f))
    }

    fun postInitialize() {
        initialized = true
    }

    override fun tick(time: Long) {
        super.tick(time)

        if (!initialized) return

        if (players.isEmpty()) {
            BlueprintEditorHandler.remove(this)
            return
        }

        players.forEach { player ->
            player.getTag(ANCHOR_MOD_TYPE_TAG)?.let { tag ->
                player.sendActionBar(Component.text("Modifying anchor: $tag"))
            }
        }
    }

    companion object {
        val ORIGIN = BlockVec(0, 100, 0)

        val ACTIVE_ANCHOR_TAG: Tag<UUID> = Tag.UUID("active_anchor")
        val ANCHOR_MOD_TYPE_TAG: Tag<AnchorModType> = Tag.Transient<AnchorModType>("anchor_mod_type")
    }
}
