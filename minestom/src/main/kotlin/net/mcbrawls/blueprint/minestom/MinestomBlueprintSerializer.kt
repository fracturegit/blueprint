package net.mcbrawls.blueprint.minestom

import com.mojang.serialization.Codec
import net.kyori.adventure.key.Key
import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.BlueprintSerializer
import net.mcbrawls.blueprint.PlacedBlueprint
import net.mcbrawls.blueprint.State
import net.mcbrawls.blueprint.Vec3i
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import java.io.File

open class MinestomBlueprintSerializer(folderRoot: File, val defaultNamespace: String? = null) : BlueprintSerializer<Block>(CODEC, folderRoot) {
    init {
        load()
    }

    operator fun get(key: Key): Blueprint<Block>? {
        return super.get(key.asString())
    }

    override operator fun get(id: String): Blueprint<Block>? {
        val namespace = defaultNamespace ?: Key.MINECRAFT_NAMESPACE
        return this[Key.key(namespace, id)]
    }

    companion object {
        val CODEC: Codec<Blueprint<Block>> = Blueprint.createCodec(::stateAsBlock, ::blockAsState)

        fun placeBlueprint(instance: Instance, point: BlockVec, blueprint: Blueprint<Block>): PlacedBlueprint<Block> {
            blueprint.forEach { offset, block ->
                placePosition(instance, point.add(BlockVec(offset.x, offset.y, offset.z)), block)
            }

            return PlacedBlueprint(blueprint, Vec3i(point.blockX, point.blockY, point.blockZ))
        }

        fun placePosition(instance: Instance, point: Point, block: Block) {
            instance.setBlock(point, block, false)
        }

        fun clear(instance: Instance, placedBlueprint: PlacedBlueprint<Block>) {
            placedBlueprint.forEachPosition { (x, y, z) ->
                placePosition(instance, BlockVec(x, y, z), Block.AIR)
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
