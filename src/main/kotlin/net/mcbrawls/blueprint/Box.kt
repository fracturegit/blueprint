package net.mcbrawls.blueprint

class Box(a: Vec3i, b: Vec3i) {
    private val min: Vec3i
    private val max: Vec3i

    init {
        val (min, max) = Vec3i.minMax(a, b)
        this.min = min
        this.max = max
    }

    val center: Vec3i = min + (max - min) * 0.5

    fun contains(x: Int, y: Int, z: Int): Boolean {
        return x in min.x..max.x &&
                y in min.y..max.y &&
                z in min.z..max.z
    }

    fun contains(vec: Vec3i): Boolean {
        return contains(vec.x, vec.y, vec.z)
    }

    fun forEach(action: (Vec3i) -> Unit) {
        (min.x..max.x).forEach { x ->
            (min.y..max.y).forEach { y ->
                (min.z..max.z).forEach { z ->
                    action.invoke(Vec3i(x, y, z))
                }
            }
        }
    }
}
