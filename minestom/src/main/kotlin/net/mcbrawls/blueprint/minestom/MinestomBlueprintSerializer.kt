package net.mcbrawls.blueprint.minestom

import com.mojang.serialization.Codec
import net.kyori.adventure.key.Key
import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.CardinalDirection
import net.mcbrawls.blueprint.PlacedBlueprint
import net.mcbrawls.blueprint.Rotation
import net.mcbrawls.blueprint.serialization.BlueprintSerializer
import net.mcbrawls.blueprint.state.State
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.joml.Vector3i
import org.joml.Vector3ic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        private val logger: Logger = LoggerFactory.getLogger(MinestomBlueprintSerializer::class.java)

        fun placeBlueprint(
            instance: Instance,
            point: BlockVec,
            blueprint: Blueprint<Block>,
            rotation: Rotation = Rotation.NONE,
        ): PlacedBlueprint<Block> {
            val size = blueprint.size
            blueprint.forEach { offset, block ->
                val (rotatedOffset, rotatedBlock) = if (rotation == Rotation.NONE) {
                    offset to block
                } else {
                    rotateBlockEntry(offset, block, rotation, size)
                }
                placePosition(
                    instance,
                    point.add(BlockVec(rotatedOffset.x(), rotatedOffset.y(), rotatedOffset.z())),
                    rotatedBlock,
                )
            }

            // Pass rotation so PlacedBlueprint.blockBox reflects the actual post-rotation footprint.
            val placedBlueprint = PlacedBlueprint(blueprint, Vector3i(point.blockX, point.blockY, point.blockZ), rotation)
            EventDispatcher.call(BlueprintPlaceEvent(instance, point, blueprint, placedBlueprint))
            return placedBlueprint
        }

        private fun rotateBlockEntry(
            pos: Vector3ic,
            block: Block,
            rotation: Rotation,
            size: Vector3ic,
        ): Pair<Vector3ic, Block> {
            val x = pos.x(); val y = pos.y(); val z = pos.z()
            val sx = size.x(); val sz = size.z()
            val newPos: Vector3ic = when (rotation) {
                Rotation.NONE -> Vector3i(x, y, z)
                Rotation.CW_90 -> Vector3i(sz - 1 - z, y, x)
                Rotation.CW_180 -> Vector3i(sx - 1 - x, y, sz - 1 - z)
                Rotation.CW_270 -> Vector3i(z, y, sx - 1 - x)
            }
            val facing = block.properties()["facing"]
            val rotatedBlock = if (facing != null) {
                val dir = CardinalDirection.entries.find { it.name.equals(facing, ignoreCase = true) }
                if (dir != null) {
                    block.withProperties(block.properties() + ("facing" to rotation.rotate(dir).name.lowercase()))
                } else block
            } else block
            return newPos to rotatedBlock
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
            val block = Block.fromKey(state.blockId)

            if (block == null) {
                logger.error("Invalid block id: ${state.blockId}")
                return Block.AIR
            }

            return block.withProperties(state.properties)
        }

        fun blockAsState(block: Block): State {
            val blockId = block.key().asString()
            return State(blockId, block.properties())
        }
    }
}
