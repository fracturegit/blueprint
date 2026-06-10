package net.mcbrawls.blueprint

import net.mcbrawls.blueprint.state.State
import net.mcbrawls.blueprint.util.NbtOps
import net.mcbrawls.codex.decodeQuick
import net.mcbrawls.codex.encodeQuick
import org.joml.Vector3i
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockDatumTest {
    @Test fun `round-trips through codec with properties`() {
        val original = BlockDatum(
            position = Vector3i(1, 2, 3),
            properties = mapOf(
                "crypt_mineable" to PropertyValue.BoolValue(true),
                "difficulty" to PropertyValue.IntValue(2),
            ),
        )
        val tag = BlockDatum.CODEC.encodeQuick(NbtOps.INSTANCE, original)
        val decoded = BlockDatum.CODEC.decodeQuick(NbtOps.INSTANCE, tag)!!
        assertEquals(original.position, decoded.position)
        assertEquals(true, decoded.getBoolean("crypt_mineable"))
        assertEquals(2, decoded.getInt("difficulty"))
    }

    @Test fun `round-trips through codec with no properties`() {
        val original = BlockDatum(position = Vector3i(0, 64, 0), properties = emptyMap())
        val tag = BlockDatum.CODEC.encodeQuick(NbtOps.INSTANCE, original)
        val decoded = BlockDatum.CODEC.decodeQuick(NbtOps.INSTANCE, tag)!!
        assertEquals(original.position, decoded.position)
        assertTrue(decoded.properties.isEmpty())
    }

    @Test fun `typed accessors return null for missing keys`() {
        val datum = BlockDatum(Vector3i(0, 0, 0), emptyMap())
        assertNull(datum.getBoolean("x"))
        assertNull(datum.getInt("x"))
        assertNull(datum.getDouble("x"))
        assertNull(datum.getString("x"))
        assertFalse("x" in datum)
    }

    @Test fun `typed accessors return null for wrong type`() {
        val datum = BlockDatum(Vector3i(0, 0, 0), mapOf("k" to PropertyValue.IntValue(1)))
        assertNull(datum.getBoolean("k"))
        assertNull(datum.getString("k"))
        assertEquals(1, datum.getInt("k"))
    }

    @Test fun `contains operator`() {
        val datum = BlockDatum(Vector3i(0, 0, 0), mapOf("foo" to PropertyValue.BoolValue(true)))
        assertTrue("foo" in datum)
        assertFalse("bar" in datum)
    }

    @Test fun `Blueprint round-trips blockData through codec`() {
        val datum = BlockDatum(Vector3i(3, 1, 2), mapOf("crypt_mineable" to PropertyValue.BoolValue(true)))
        val blueprint = Blueprint(
            palette = emptyList<String>(),
            palettedStates = emptyList(),
            markers = emptyMap(),
            regions = emptyMap(),
            blockData = listOf(datum),
        )
        val codec = Blueprint.createCodec<String>({ it.blockId }, { State(it) })
        val tag = codec.encodeQuick(NbtOps.INSTANCE, blueprint)
        val decoded = codec.decodeQuick(NbtOps.INSTANCE, tag)!!
        assertEquals(1, decoded.blockData.size)
        assertEquals(datum.position, decoded.blockData[0].position)
        assertEquals(true, decoded.blockData[0].getBoolean("crypt_mineable"))
    }

    @Test fun `PlacedBlueprint getBlockData returns data at world position`() {
        val datum = BlockDatum(
            position = Vector3i(1, 0, 2),
            properties = mapOf("crypt_mineable" to PropertyValue.BoolValue(true)),
        )
        val blueprint = Blueprint(
            palette = emptyList<Any>(),
            palettedStates = emptyList(),
            markers = emptyMap(),
            regions = emptyMap(),
            blockData = listOf(datum),
        )
        @Suppress("UNCHECKED_CAST")
        val placed = PlacedBlueprint(blueprint as Blueprint<Any>, Vector3i(10, 64, 10))
        // local (1,0,2) + origin (10,64,10) = world (11,64,12)
        val result = placed.getBlockData(Vector3i(11, 64, 12))
        assertNotNull(result)
        assertEquals(true, result!!.getBoolean("crypt_mineable"))
    }

    @Test fun `PlacedBlueprint getBlockData returns null for position with no data`() {
        val blueprint = Blueprint(
            palette = emptyList<Any>(),
            palettedStates = emptyList(),
            markers = emptyMap(),
            regions = emptyMap(),
        )
        @Suppress("UNCHECKED_CAST")
        val placed = PlacedBlueprint(blueprint as Blueprint<Any>, Vector3i(0, 0, 0))
        assertNull(placed.getBlockData(Vector3i(5, 5, 5)))
    }

    @Test fun `PlacedBlueprint getAllBlockData returns world-coord datums`() {
        val datum = BlockDatum(Vector3i(0, 0, 0), mapOf("k" to PropertyValue.IntValue(7)))
        val blueprint = Blueprint(
            palette = emptyList<Any>(),
            palettedStates = emptyList(),
            markers = emptyMap(),
            regions = emptyMap(),
            blockData = listOf(datum),
        )
        @Suppress("UNCHECKED_CAST")
        val placed = PlacedBlueprint(blueprint as Blueprint<Any>, Vector3i(5, 5, 5))
        val all = placed.getAllBlockData()
        assertEquals(1, all.size)
        assertEquals(Vector3i(5, 5, 5), all[0].position)
        assertEquals(7, all[0].getInt("k"))
    }
}
