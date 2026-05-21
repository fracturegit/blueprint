package net.mcbrawls.blueprint

import net.kyori.adventure.key.Key
import net.mcbrawls.blueprint.util.NbtOps
import net.mcbrawls.codex.decodeQuick
import net.mcbrawls.codex.encodeQuick
import org.joml.Vector2f
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DecorationCodecTest {
    @Test fun `decoration round-trips through codec with no properties`() {
        val original = Decoration(
            type = Key.key("fracture", "jump_pad"),
            position = Vector3d(1.0, 64.0, -3.5),
            rotation = Vector2f(90f, 0f),
        )
        val tag = Decoration.CODEC.encodeQuick(NbtOps.INSTANCE, original)
        val decoded = Decoration.CODEC.decodeQuick(NbtOps.INSTANCE, tag)
        assertEquals(original.type, decoded!!.type)
        assertEquals(original.position.x(), decoded.position.x(), 1e-6)
        assertEquals(original.position.y(), decoded.position.y(), 1e-6)
        assertEquals(original.position.z(), decoded.position.z(), 1e-6)
        assertEquals(original.rotation.x(), decoded.rotation.x(), 1e-4f)
        assertEquals(original.rotation.y(), decoded.rotation.y(), 1e-4f)
    }

    @Test fun `decoration round-trips through codec with properties`() {
        val original = Decoration(
            type = Key.key("fracture", "jump_pad"),
            position = Vector3d(0.0, 64.0, 0.0),
            rotation = Vector2f(0f, 0f),
            properties = mapOf(
                "vertical_power" to PropertyValue.DoubleValue(4.0),
                "horizontal_power" to PropertyValue.DoubleValue(2.5),
            ),
        )
        val tag = Decoration.CODEC.encodeQuick(NbtOps.INSTANCE, original)
        val decoded = Decoration.CODEC.decodeQuick(NbtOps.INSTANCE, tag)
        assertEquals(2, decoded!!.properties.size)
        assertEquals(4.0, (decoded.properties["vertical_power"] as PropertyValue.DoubleValue).value, 1e-6)
    }

    @Test fun `PlacedBlueprint getDecorations offsets position by placement origin`() {
        // Decoration at local (1, 0, 1), placed blueprint origin at (10, 64, 10)
        val decoration = Decoration(
            type = Key.key("fracture", "jump_pad"),
            position = Vector3d(1.0, 0.0, 1.0),
            rotation = Vector2f(0f, 0f),
        )
        val blueprint = Blueprint(
            palette = emptyList<Any>(),
            palettedStates = emptyList(),
            markers = emptyMap(),
            regions = emptyMap(),
            decorations = listOf(decoration),
        )
        // Cast is needed because Blueprint is generic; use Any as T for this test
        @Suppress("UNCHECKED_CAST")
        val placed = PlacedBlueprint(blueprint as Blueprint<Any>, org.joml.Vector3i(10, 64, 10))
        val result = placed.getDecorations()
        assertEquals(1, result.size)
        assertEquals(11.0, result[0].position.x(), 1e-6)
        assertEquals(64.0, result[0].position.y(), 1e-6)
        assertEquals(11.0, result[0].position.z(), 1e-6)
    }
}
