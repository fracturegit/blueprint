package net.mcbrawls.blueprint.editor.region

/**
 * Represents a colored dust particle for visualization.
 */
data class DustColor(val red: Float, val green: Float, val blue: Float) {
    companion object {
        val RED = DustColor(1f, 0f, 0f)
        val GREEN = DustColor(0f, 1f, 0f)
        val BLUE = DustColor(0f, 0f, 1f)
        val YELLOW = DustColor(1f, 1f, 0f)
        val CYAN = DustColor(0f, 1f, 1f)
        val MAGENTA = DustColor(1f, 0f, 1f)
    }
}
