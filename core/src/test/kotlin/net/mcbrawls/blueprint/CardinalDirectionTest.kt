package net.mcbrawls.blueprint

import org.joml.Vector3i
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardinalDirectionTest {

    @Test fun `opposite of NORTH is SOUTH`() {
        assertEquals(CardinalDirection.SOUTH, CardinalDirection.NORTH.opposite())
    }

    @Test fun `opposite of SOUTH is NORTH`() {
        assertEquals(CardinalDirection.NORTH, CardinalDirection.SOUTH.opposite())
    }

    @Test fun `opposite of EAST is WEST`() {
        assertEquals(CardinalDirection.WEST, CardinalDirection.EAST.opposite())
    }

    @Test fun `opposite of WEST is EAST`() {
        assertEquals(CardinalDirection.EAST, CardinalDirection.WEST.opposite())
    }

    @Test fun `NORTH step is negative Z`() {
        assertEquals(Vector3i(0, 0, -1), CardinalDirection.NORTH.step)
    }

    @Test fun `SOUTH step is positive Z`() {
        assertEquals(Vector3i(0, 0, 1), CardinalDirection.SOUTH.step)
    }

    @Test fun `EAST step is positive X`() {
        assertEquals(Vector3i(1, 0, 0), CardinalDirection.EAST.step)
    }

    @Test fun `WEST step is negative X`() {
        assertEquals(Vector3i(-1, 0, 0), CardinalDirection.WEST.step)
    }

    @Test fun `Rotation CW_90 rotates NORTH to EAST`() {
        assertEquals(CardinalDirection.EAST, Rotation.CW_90.rotate(CardinalDirection.NORTH))
    }

    @Test fun `Rotation CW_90 rotates EAST to SOUTH`() {
        assertEquals(CardinalDirection.SOUTH, Rotation.CW_90.rotate(CardinalDirection.EAST))
    }

    @Test fun `Rotation CW_180 rotates NORTH to SOUTH`() {
        assertEquals(CardinalDirection.SOUTH, Rotation.CW_180.rotate(CardinalDirection.NORTH))
    }

    @Test fun `Rotation CW_270 rotates NORTH to WEST`() {
        assertEquals(CardinalDirection.WEST, Rotation.CW_270.rotate(CardinalDirection.NORTH))
    }

    @Test fun `Rotation NONE does not change direction`() {
        CardinalDirection.entries.forEach { dir ->
            assertEquals(dir, Rotation.NONE.rotate(dir))
        }
    }

    @Test fun `four CW_90 rotations return to original`() {
        CardinalDirection.entries.forEach { dir ->
            var result = dir
            repeat(4) { result = Rotation.CW_90.rotate(result) }
            assertEquals(dir, result)
        }
    }

    @Test fun `rotatedSize swaps x and z for CW_90`() {
        val size = Vector3i(10, 5, 8)
        val rotated = Rotation.CW_90.rotatedSize(size)
        assertEquals(8, rotated.x())
        assertEquals(5, rotated.y())
        assertEquals(10, rotated.z())
    }

    @Test fun `rotatedSize is unchanged for CW_180`() {
        val size = Vector3i(10, 5, 8)
        val rotated = Rotation.CW_180.rotatedSize(size)
        assertEquals(10, rotated.x())
        assertEquals(5, rotated.y())
        assertEquals(8, rotated.z())
    }

    @Test fun `CW_270 rotatedSize swaps X and Z`() {
        val original = Vector3i(5, 3, 10)
        val result = Rotation.CW_270.rotatedSize(original)
        assertEquals(Vector3i(10, 3, 5), result)
    }
}
