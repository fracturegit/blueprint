package net.mcbrawls.blueprint.minestom

import com.mojang.serialization.Codec
import net.kyori.adventure.key.Key
import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.PlacedBlueprint
import net.mcbrawls.blueprint.serialization.BlueprintSerializer
import net.mcbrawls.blueprint.state.State
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.joml.Vector3i
import java.io.File

open class MinestomBlueprintSerializer(folderRoot: File, defaultNamespace: String? = null) : BlueprintSerializer<Block>(CODEC, folderRoot) {
    open val name: String = this::class.simpleName ?: defaultNamespace ?: "Default Serializer"

    operator fun get(key: Key): Blueprint<Block>? {
        return super.get(key.asString())
    }

    private val namespace = defaultNamespace ?: Key.MINECRAFT_NAMESPACE

    override operator fun get(id: String): Blueprint<Block>? {
        return this[Key.key(namespace, id)]
    }

    fun getRaw(id: String): Blueprint<Block>? {
        return super.get(id)
    }

    fun getDefaultedFile(id: String): File {
        val key = Key.key(namespace, id)
        return getFile(key.toString())
    }

    companion object {
        val CODEC: Codec<Blueprint<Block>> = Blueprint.createCodec(::stateAsBlock, ::blockAsState)

        fun placeBlueprint(instance: Instance, point: BlockVec, blueprint: Blueprint<Block>): PlacedBlueprint<Block> {
            blueprint.forEach { offset, block ->
                placePosition(instance, point.add(BlockVec(offset.x(), offset.y(), offset.z())), block)
            }

            val placedBlueprint = PlacedBlueprint(blueprint, Vector3i(point.blockX, point.blockY, point.blockZ))
            EventDispatcher.call(BlueprintPlaceEvent(instance, point, blueprint, placedBlueprint))

            return placedBlueprint
        }

        fun placePosition(instance: Instance, point: Point, block: Block) {
            instance.setBlock(point, block, false)
        }

        fun clear(instance: Instance, placedBlueprint: PlacedBlueprint<Block>) {
            placedBlueprint.forEachPosition { pos ->
                placePosition(instance, BlockVec(pos.x(), pos.y(), pos.z()), Block.AIR)
            }
        }

        fun stateAsBlock(state: State): Block {
            val block = Block.fromKey(state.blockId) ?: error("Invalid block id: ${state.blockId}")
            return block.withProperties(state.properties)
        }

        fun blockAsState(block: Block): State {
            val blockId = block.key().asString()
            return State(blockId, block.properties())
        }
    }
}
