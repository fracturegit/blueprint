package net.mcbrawls.blueprint

import org.joml.Vector3i
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoomConnectorTest {
    private val size10x5x8 = Vector3i(10, 5, 8)

    private fun connector(x: Int, y: Int, z: Int, dir: CardinalDirection, type: ConnectorType = ConnectorType.EXIT) =
        RoomConnector(Vector3i(x, y, z), dir, type)

    @Test fun `NONE rotation does not change position or direction`() {
        val c = connector(3, 0, 0, CardinalDirection.NORTH)
        val rotated = c.rotate(Rotation.NONE, size10x5x8)
        assertEquals(Vector3i(3, 0, 0), Vector3i(rotated.position))
        assertEquals(CardinalDirection.NORTH, rotated.direction)
    }

    @Test fun `CW_90 rotates position correctly`() {
        // (x, y, z) -> (sz - 1 - z, y, x)  with sz=8
        val c = connector(3, 2, 1, CardinalDirection.NORTH)
        val rotated = c.rotate(Rotation.CW_90, size10x5x8)
        assertEquals(Vector3i(8 - 1 - 1, 2, 3), Vector3i(rotated.position)) // (6, 2, 3)
        assertEquals(CardinalDirection.EAST, rotated.direction)
    }

    @Test fun `CW_180 rotates position correctly`() {
        // (x, y, z) -> (sx - 1 - x, y, sz - 1 - z)  with sx=10, sz=8
        val c = connector(3, 2, 1, CardinalDirection.EAST)
        val rotated = c.rotate(Rotation.CW_180, size10x5x8)
        assertEquals(Vector3i(10 - 1 - 3, 2, 8 - 1 - 1), Vector3i(rotated.position)) // (6, 2, 6)
        assertEquals(CardinalDirection.WEST, rotated.direction)
    }

    @Test fun `CW_270 rotates position correctly`() {
        // (x, y, z) -> (z, y, sx - 1 - x)  with sx=10
        val c = connector(3, 2, 1, CardinalDirection.SOUTH)
        val rotated = c.rotate(Rotation.CW_270, size10x5x8)
        assertEquals(Vector3i(1, 2, 10 - 1 - 3), Vector3i(rotated.position)) // (1, 2, 6)
        assertEquals(CardinalDirection.EAST, rotated.direction)
    }

    @Test fun `type is preserved through rotation`() {
        val c = connector(0, 0, 0, CardinalDirection.NORTH, ConnectorType.ENTRANCE)
        assertEquals(ConnectorType.ENTRANCE, c.rotate(Rotation.CW_90, size10x5x8).type)
    }

    @Test fun `four CW_90 rotations return to original`() {
        val squareSize = Vector3i(8, 5, 8)
        val original = RoomConnector(Vector3i(3, 2, 1), CardinalDirection.EAST, ConnectorType.EXIT)
        var result = original
        repeat(4) { result = result.rotate(Rotation.CW_90, squareSize) }
        assertEquals(Vector3i(original.position), Vector3i(result.position))
        assertEquals(original.direction, result.direction)
    }

    @Test fun `BOTH type is preserved through rotation`() {
        val c = RoomConnector(Vector3i(0, 0, 0), CardinalDirection.NORTH, ConnectorType.BOTH)
        assertEquals(ConnectorType.BOTH, c.rotate(Rotation.CW_90, size10x5x8).type)
    }

    @Test fun `role predicates`() {
        assertEquals(true, ConnectorType.ENTRANCE.isEntrance)
        assertEquals(false, ConnectorType.ENTRANCE.isExit)
        assertEquals(false, ConnectorType.EXIT.isEntrance)
        assertEquals(true, ConnectorType.EXIT.isExit)
        assertEquals(true, ConnectorType.BOTH.isEntrance)
        assertEquals(true, ConnectorType.BOTH.isExit)
    }
}
