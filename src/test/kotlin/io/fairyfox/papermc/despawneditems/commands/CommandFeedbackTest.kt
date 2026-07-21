package io.fairyfox.papermc.despawneditems.commands

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.TargetingPlayerMock
import io.fairyfox.papermc.despawneditems.plain
import org.bukkit.Material
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandFeedbackTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private lateinit var player: TargetingPlayerMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
        player = TargetingPlayerMock(server, "Tester")
        server.addPlayer(player)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `error success warning info all deliver their message`() {
        CommandFeedback.error(player, "boom")
        assertEquals("ERROR: boom", player.nextMessage().plain())
        CommandFeedback.success(player, "yay")
        assertEquals("yay", player.nextMessage().plain())
        CommandFeedback.warning(player, "careful")
        assertEquals("WARNING: careful", player.nextMessage().plain())
        CommandFeedback.info(player, "fyi")
        assertEquals("fyi", player.nextMessage().plain())
    }

    @Test
    fun `targetBlock returns the looked-at block location`() {
        val block = world.getBlockAt(3, 64, 3)
        block.type = Material.STONE
        player.target = block

        val location = CommandFeedback.targetBlock(player)
        assertNotNull(location)
        assertEquals(3, location.blockX)
        assertEquals(64, location.blockY)
        assertEquals(3, location.blockZ)
    }

    @Test
    fun `targetBlock errors when no block in range`() {
        player.target = null
        assertNull(CommandFeedback.targetBlock(player))
        assertTrue(player.nextMessage().plain().startsWith("ERROR:"), "should send a range error")
    }

    @Test
    fun `targetBlock errors when the target is air`() {
        val block = world.getBlockAt(0, 90, 0) // untouched blocks default to air
        player.target = block
        assertNull(CommandFeedback.targetBlock(player))
        assertTrue(player.nextMessage().plain().startsWith("ERROR:"))
    }
}
