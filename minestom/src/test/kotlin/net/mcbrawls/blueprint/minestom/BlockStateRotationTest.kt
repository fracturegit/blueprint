package net.mcbrawls.blueprint.minestom

import net.mcbrawls.blueprint.Rotation
import net.mcbrawls.blueprint.minestom.MinestomBlueprintSerializer.Companion.rotateBlockState
import net.minestom.server.instance.block.Block
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class BlockStateRotationTest {
    @Test
    fun `NONE rotation returns the same block instance`() {
        val block = Block.COBBLESTONE_WALL.withProperty("north", "tall")
        assertSame(block, rotateBlockState(block, Rotation.NONE))
    }

    @Test
    fun `cardinal facing rotates clockwise`() {
        val block = Block.FURNACE.withProperty("facing", "north")
        val rotated = rotateBlockState(block, Rotation.CW_90)
        assertEquals("east", rotated.getProperty("facing"))
    }

    @Test
    fun `non-cardinal facing is left unchanged`() {
        val block = Block.OBSERVER.withProperty("facing", "up")
        val rotated = rotateBlockState(block, Rotation.CW_90)
        assertEquals("up", rotated.getProperty("facing"))
    }

    @Test
    fun `stair shape is untouched while facing rotates`() {
        val block = Block.OAK_STAIRS.withProperties(mapOf("facing" to "north", "shape" to "inner_left"))
        val rotated = rotateBlockState(block, Rotation.CW_90)
        assertEquals("east", rotated.getProperty("facing"))
        assertEquals("inner_left", rotated.getProperty("shape"))
    }

    @Test
    fun `wall connections rotate 90 degrees clockwise`() {
        val block = Block.COBBLESTONE_WALL.withProperties(
            mapOf("north" to "tall", "east" to "none", "south" to "none", "west" to "low", "up" to "true"),
        )
        val rotated = rotateBlockState(block, Rotation.CW_90)
        assertEquals("tall", rotated.getProperty("east"))
        assertEquals("none", rotated.getProperty("south"))
        assertEquals("none", rotated.getProperty("west"))
        assertEquals("low", rotated.getProperty("north"))
        assertEquals("true", rotated.getProperty("up"))
    }

    @Test
    fun `fence connections rotate 180 degrees`() {
        val block = Block.OAK_FENCE.withProperties(
            mapOf("north" to "true", "east" to "false", "south" to "false", "west" to "false"),
        )
        val rotated = rotateBlockState(block, Rotation.CW_180)
        assertEquals("true", rotated.getProperty("south"))
        assertEquals("false", rotated.getProperty("north"))
        assertEquals("false", rotated.getProperty("east"))
        assertEquals("false", rotated.getProperty("west"))
    }

    @Test
    fun `pane connections rotate 270 degrees`() {
        val block = Block.GLASS_PANE.withProperties(
            mapOf("north" to "true", "east" to "false", "south" to "false", "west" to "false"),
        )
        val rotated = rotateBlockState(block, Rotation.CW_270)
        assertEquals("true", rotated.getProperty("west"))
        assertEquals("false", rotated.getProperty("north"))
        assertEquals("false", rotated.getProperty("east"))
        assertEquals("false", rotated.getProperty("south"))
    }

    @Test
    fun `horizontal axis swaps on odd turns`() {
        val log = Block.OAK_LOG.withProperty("axis", "x")
        assertEquals("z", rotateBlockState(log, Rotation.CW_90).getProperty("axis"))
        assertEquals("x", rotateBlockState(log, Rotation.CW_180).getProperty("axis"))
        assertEquals("z", rotateBlockState(log, Rotation.CW_270).getProperty("axis"))
    }

    @Test
    fun `vertical axis is untouched`() {
        val log = Block.OAK_LOG.withProperty("axis", "y")
        assertEquals("y", rotateBlockState(log, Rotation.CW_90).getProperty("axis"))
    }

    @Test
    fun `straight rail shape rotates`() {
        val rail = Block.RAIL.withProperty("shape", "north_south")
        assertEquals("east_west", rotateBlockState(rail, Rotation.CW_90).getProperty("shape"))
        assertEquals("north_south", rotateBlockState(rail, Rotation.CW_180).getProperty("shape"))
    }

    @Test
    fun `ascending rail shape rotates`() {
        val rail = Block.POWERED_RAIL.withProperty("shape", "ascending_north")
        assertEquals("ascending_east", rotateBlockState(rail, Rotation.CW_90).getProperty("shape"))
        assertEquals("ascending_south", rotateBlockState(rail, Rotation.CW_180).getProperty("shape"))
        assertEquals("ascending_west", rotateBlockState(rail, Rotation.CW_270).getProperty("shape"))
    }

    @Test
    fun `corner rail shape rotates`() {
        val rail = Block.RAIL.withProperty("shape", "south_east")
        assertEquals("south_west", rotateBlockState(rail, Rotation.CW_90).getProperty("shape"))
        assertEquals("north_west", rotateBlockState(rail, Rotation.CW_180).getProperty("shape"))
        assertEquals("north_east", rotateBlockState(rail, Rotation.CW_270).getProperty("shape"))
    }

    @Test
    fun `standing sign rotation value shifts by 4 per turn`() {
        val sign = Block.OAK_SIGN.withProperty("rotation", "3")
        assertEquals("7", rotateBlockState(sign, Rotation.CW_90).getProperty("rotation"))
        assertEquals("11", rotateBlockState(sign, Rotation.CW_180).getProperty("rotation"))
        val sign2 = Block.OAK_SIGN.withProperty("rotation", "14")
        assertEquals("2", rotateBlockState(sign2, Rotation.CW_90).getProperty("rotation"))
    }
}
