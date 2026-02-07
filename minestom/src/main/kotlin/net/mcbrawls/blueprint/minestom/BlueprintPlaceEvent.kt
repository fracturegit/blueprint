package net.mcbrawls.blueprint.minestom

import net.mcbrawls.blueprint.Blueprint
import net.mcbrawls.blueprint.PlacedBlueprint
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

class BlueprintPlaceEvent(
    private val _instance: Instance,
    val point: BlockVec,
    val blueprint: Blueprint<Block>,
    val placedBlueprint: PlacedBlueprint<Block>
) : InstanceEvent {
    override fun getInstance(): Instance {
        return _instance
    }
}
