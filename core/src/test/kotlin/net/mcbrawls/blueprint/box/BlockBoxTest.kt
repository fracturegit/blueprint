package net.mcbrawls.blueprint.box

import org.joml.Vector3i
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockBoxTest {

    @Test fun `overlapping boxes intersect`() {
        val a = BlockBox(Vector3i(0, 0, 0), Vector3i(5, 5, 5))
        val b = BlockBox(Vector3i(3, 3, 3), Vector3i(8, 8, 8))
        assertTrue(a.intersects(b))
        assertTrue(b.intersects(a))
    }

    @Test fun `face-touching boxes intersect`() {
        val a = BlockBox(Vector3i(0, 0, 0), Vector3i(5, 5, 5))
        val b = BlockBox(Vector3i(5, 0, 0), Vector3i(10, 5, 5))
        assertTrue(a.intersects(b))
        assertTrue(b.intersects(a))
    }

    @Test fun `separated boxes do not intersect`() {
        val a = BlockBox(Vector3i(0, 0, 0), Vector3i(4, 4, 4))
        val b = BlockBox(Vector3i(6, 0, 0), Vector3i(10, 4, 4))
        assertFalse(a.intersects(b))
        assertFalse(b.intersects(a))
    }

    @Test fun `contained box intersects outer box`() {
        val outer = BlockBox(Vector3i(0, 0, 0), Vector3i(10, 10, 10))
        val inner = BlockBox(Vector3i(2, 2, 2), Vector3i(5, 5, 5))
        assertTrue(outer.intersects(inner))
        assertTrue(inner.intersects(outer))
    }

    @Test fun `non-overlapping on X axis only does not intersect`() {
        val a = BlockBox(Vector3i(0, 0, 0), Vector3i(4, 10, 10))
        val b = BlockBox(Vector3i(6, 0, 0), Vector3i(10, 10, 10))
        assertFalse(a.intersects(b))
        assertFalse(b.intersects(a))
    }
}
