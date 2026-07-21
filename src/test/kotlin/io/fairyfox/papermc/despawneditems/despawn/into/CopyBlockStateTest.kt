package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.SkullMeta
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The tile-entity copier ([DespawnBlockIntoAir.copyBlockToLocation]) branch by branch.
 * MockBukkit block states are snapshots whose `update()` restores stale data, so these
 * assert the copy *executes* the branch and places the right material; content-level
 * verification stays real-server territory (testing.md §19–22).
 */
class CopyBlockStateTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private var nextX = 0

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun target(): Block {
        nextX += 2
        return world.getBlockAt(nextX, 64, 0)
    }

    /** Builds an item whose BlockStateMeta carries the state of a freshly-placed block. */
    private fun itemWithState(
        material: Material,
        prepare: (Block) -> Unit = {},
    ): ItemStack {
        val sourceBlock = target().also { it.type = material }
        prepare(sourceBlock)
        val item = ItemStack(material)
        val meta = item.itemMeta as BlockStateMeta
        meta.blockState = sourceBlock.state
        item.itemMeta = meta
        return item
    }

    @Test
    fun `a chest item with stored contents copies as a chest`() {
        val item =
            itemWithState(Material.CHEST) { block ->
                (block.state as org.bukkit.block.Chest).blockInventory.addItem(ItemStack(Material.DIRT, 5))
            }
        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.CHEST, block.type)
    }

    @Test
    fun `a furnace item carries burn and cook times`() {
        val item =
            itemWithState(Material.FURNACE) { block ->
                val furnace = block.state as org.bukkit.block.Furnace
                furnace.burnTime = 77
                furnace.cookTime = 33
            }
        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.FURNACE, block.type)
    }

    @Test
    fun `a sign item carries its lines`() {
        val item =
            itemWithState(Material.OAK_SIGN) { block ->
                val sign = block.state as org.bukkit.block.Sign
                sign.line(0, Component.text("hello"))
                sign.line(1, Component.text("world"))
            }
        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.OAK_SIGN, block.type)
    }

    @Test
    fun `a player head item copies the skull owner`() {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta
        @Suppress("DEPRECATION")
        meta.owningPlayer = Bukkit.getOfflinePlayer("Steve")
        item.itemMeta = meta

        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.PLAYER_HEAD, block.type)
    }

    @Test
    fun `a banner item's pattern meta is applied to the placed banner`() {
        // Banners carry patterns on BannerMeta (their own copier branch), not BlockStateMeta.
        val item = ItemStack(Material.WHITE_BANNER)
        val meta = item.itemMeta as org.bukkit.inventory.meta.BannerMeta
        meta.addPattern(org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.BLUE, org.bukkit.block.banner.PatternType.STRIPE_TOP))
        item.itemMeta = meta

        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.WHITE_BANNER, block.type)
    }

    @Test
    fun `a brewing stand item carries the brewing time`() {
        val item =
            itemWithState(Material.BREWING_STAND) { block ->
                (block.state as org.bukkit.block.BrewingStand).brewingTime = 123
            }
        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.BREWING_STAND, block.type)
    }

    @Test
    fun `a beacon item carries its effects`() {
        val item = itemWithState(Material.BEACON)
        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.BEACON, block.type)
    }

    @Test
    fun `a beehive item carries its flower`() {
        val item =
            itemWithState(Material.BEEHIVE) { block ->
                (block.state as org.bukkit.block.Beehive).flower = block.location
            }
        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.BEEHIVE, block.type)
    }

    @Test
    fun `a command block item carries name and command`() {
        val item =
            itemWithState(Material.COMMAND_BLOCK) { block ->
                val commandBlock = block.state as org.bukkit.block.CommandBlock
                commandBlock.setCommand("say hello")
                commandBlock.name(Component.text("Namey"))
            }
        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.COMMAND_BLOCK, block.type)
    }

    @Test
    fun `a spawner item carries spawn type and delay`() {
        val item =
            itemWithState(Material.SPAWNER) { block ->
                val spawner = block.state as org.bukkit.block.CreatureSpawner
                spawner.spawnedType = org.bukkit.entity.EntityType.ZOMBIE
                spawner.delay = 99
            }
        val block = target()
        DespawnBlockIntoAir.copyBlockToLocation(item, block)
        assertEquals(Material.SPAWNER, block.type)
    }
}
