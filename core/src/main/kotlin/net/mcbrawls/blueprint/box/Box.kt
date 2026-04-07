package net.mcbrawls.blueprint.box

abstract class Box<V : Any, N : Number, B : Box<V, N, B>> {
    abstract val min: V
    abstract val max: V
    abstract val center: V
    abstract val size: N

    abstract fun offset(vec: V): B
    abstract fun contains(vec: V): Boolean
    abstract fun contains(x: N, y: N, z: N): Boolean
}
