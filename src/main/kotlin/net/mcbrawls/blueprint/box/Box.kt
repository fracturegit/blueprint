package net.mcbrawls.blueprint.box

abstract class Box<V : Any, N : Number> {
    abstract val min: V
    abstract val max: V
    abstract val center: V
    
    abstract fun contains(vec: V): Boolean
    abstract fun contains(x: N, y: N, z: N): Boolean
}
