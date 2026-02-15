package net.mcbrawls.blueprint.editor

import net.mcbrawls.blueprint.Anchor
import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.box.BlockBox
import net.mcbrawls.blueprint.box.VecBox
import net.mcbrawls.blueprint.editor.anchor.AnchorEntity
import net.mcbrawls.blueprint.state.PalettedState
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.joml.Vector3i
import org.joml.Vector3ic

object MinestomBlueprintHelper {
    fun createBlueprint(root: BlockVec, blocks: Map<Vector3ic, Block>, anchorEntities: Collection<AnchorEntity> = emptyList(), regions: Map<String, VecBox> = emptyMap()): Blueprint<Block> {
        val palette = mutableListOf<Block>()
        val palettedStates = mutableListOf<PalettedState>()

        blocks.forEach { (position, block) ->
            if (block !in palette) {
                palette.add(block)
            }

            // create paletted state
            val derivedPosition = Vector3i(position.x() - root.blockX, position.y() - root.blockY, position.z() - root.blockZ)
            val paletteId = palette.indexOf(block)
            palettedStates.add(PalettedState(derivedPosition, paletteId))
        }

        // create anchors
        val anchors = mutableListOf<Pair<String, Anchor>>()
        anchorEntities.forEach { anchorEntity ->
            val id = anchorEntity.anchorId
            val anchor = anchorEntity.createAnchor(root)
            anchors.add(id to anchor)
        }

        val blueprint = Blueprint(palette, palettedStates, anchors, regions)
        return blueprint
    }


    fun getBlocks(instance: Instance, bounds: Bounds): Map<Vector3ic, Block> {
        val min = bounds.min
        val max = bounds.max
        val box = BlockBox(
            Vector3i(min.blockX, min.blockY, min.blockZ),
            Vector3i(max.blockX, max.blockY, max.blockZ),
        )

        return buildMap {
            box.forEach { position ->
                val block = instance.getBlock(position.x(), position.y(), position.z())

                if (block.isAir) return@forEach

                this[position] = block
            }
        }
    }
}
