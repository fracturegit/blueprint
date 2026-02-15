package net.mcbrawls.blueprint.editor

import net.mcbrawls.blueprint.box.VecBox
import net.minestom.server.coordinate.BlockVec
import kotlin.math.max
import kotlin.math.min

class Bounds(min: BlockVec, max: BlockVec) {
    var min: BlockVec = min
        private set

    var max: BlockVec = max
        private set

    constructor(origin: BlockVec) : this(origin, origin)

    constructor(box: VecBox) : this(
        BlockVec(box.min.x(), box.min.y(), box.min.z()),
        BlockVec(box.max.x(), box.max.y(), box.max.z()),
    )

    fun update(x: Int, y: Int, z: Int) {
        min = BlockVec(
            min(min.blockX, x),
            min(min.blockY, y),
            min(min.blockZ, z)
        )
        max = BlockVec(
            max(max.blockX, x),
            max(max.blockY, y),
            max(max.blockZ, z)
        )
    }
}
