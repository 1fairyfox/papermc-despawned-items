package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DespawnItemIntoEntityTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock
    private lateinit var strategy: DespawnItemIntoEntity
    private var nextX = 0

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
        strategy = DespawnItemIntoEntity(plugin)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun processFor(item: ItemStack) = DespawnProcess(item, plugin)

    /** A fresh air block with exactly one entity of [type] standing in it. */
    private fun blockWith(type: EntityType): Pair<Block, org.bukkit.entity.Entity> {
        nextX += 8
        val block = world.getBlockAt(nextX, 64, 0)
        val entity = world.spawnEntity(Location(world, nextX + 0.5, 64.5, 0.5), type)
        return block to entity
    }

    @Test
    fun `applies only to air occupied by exactly one usable entity`() {
        val (frameBlock, _) = blockWith(EntityType.ITEM_FRAME)
        assertTrue(strategy.doesApply(frameBlock))

        val (zombieBlock, _) = blockWith(EntityType.ZOMBIE)
        assertTrue(strategy.doesApply(zombieBlock))

        val (cartBlock, _) = blockWith(EntityType.CHEST_MINECART)
        assertTrue(strategy.doesApply(cartBlock))

        // No entity at all.
        nextX += 8
        assertFalse(strategy.doesApply(world.getBlockAt(nextX, 64, 0)))

        // Two entities in the same spot.
        val (crowdedBlock, _) = blockWith(EntityType.ZOMBIE)
        world.spawnEntity(Location(world, crowdedBlock.x + 0.5, 64.5, 0.5), EntityType.ZOMBIE)
        assertFalse(strategy.doesApply(crowdedBlock))

        // A solid block under the entity.
        val (solidBlock, _) = blockWith(EntityType.ZOMBIE)
        solidBlock.type = Material.STONE
        assertFalse(strategy.doesApply(solidBlock))
    }

    @Test
    fun `an empty item frame takes one item`() {
        val (block, entity) = blockWith(EntityType.ITEM_FRAME)
        val frame = entity as ItemFrame

        val process = processFor(ItemStack(Material.DIRT, 3))
        assertEquals(DespawnIntoResult.PARTIALLY, strategy.despawnInto(process, block))
        assertEquals(Material.DIRT, frame.item.type)
        assertEquals(2, process.item?.amount)

        // Occupied frame takes nothing.
        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(processFor(ItemStack(Material.STONE)), block))
    }

    @Test
    fun `a single item into a frame is FULLY`() {
        val (block, _) = blockWith(EntityType.ITEM_FRAME)
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(processFor(ItemStack(Material.DIRT, 1)), block))
    }

    @Test
    fun `living entity equipment fills in slot order`() {
        val (block, entity) = blockWith(EntityType.ZOMBIE)
        val equipment = (entity as LivingEntity).equipment!!

        // 1: anything empty-helmet goes on the head.
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(processFor(ItemStack(Material.DIRT, 1)), block))
        assertEquals(Material.DIRT, equipment.helmet?.type)

        // 2: chestplate by name.
        assertEquals(
            DespawnIntoResult.FULLY,
            strategy.despawnInto(processFor(ItemStack(Material.IRON_CHESTPLATE, 1)), block),
        )
        assertEquals(Material.IRON_CHESTPLATE, equipment.chestplate?.type)

        // 3: leggings by name.
        assertEquals(
            DespawnIntoResult.FULLY,
            strategy.despawnInto(processFor(ItemStack(Material.IRON_LEGGINGS, 1)), block),
        )
        assertEquals(Material.IRON_LEGGINGS, equipment.leggings?.type)

        // 4: boots by name.
        assertEquals(
            DespawnIntoResult.FULLY,
            strategy.despawnInto(processFor(ItemStack(Material.IRON_BOOTS, 1)), block),
        )
        assertEquals(Material.IRON_BOOTS, equipment.boots?.type)

        // 5: main hand, then 6: off hand.
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(processFor(ItemStack(Material.STICK, 1)), block))
        assertEquals(Material.STICK, equipment.itemInMainHand.type)
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(processFor(ItemStack(Material.BONE, 1)), block))
        assertEquals(Material.BONE, equipment.itemInOffHand.type)

        // 7: everything full → NONE.
        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(processFor(ItemStack(Material.DIRT)), block))
    }

    @Test
    fun `an armor stand grows arms when handed an item`() {
        val (block, entity) = blockWith(EntityType.ARMOR_STAND)
        val stand = entity as ArmorStand
        stand.equipment.setHelmet(ItemStack(Material.DIRT)) // occupy the helmet slot

        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(processFor(ItemStack(Material.STICK, 1)), block))
        assertTrue(stand.hasArms(), "arms must be enabled so the item is visible")
        assertEquals(Material.STICK, stand.equipment.itemInMainHand.type)
    }

    @Test
    fun `a storage minecart takes the stack into its inventory`() {
        val (block, entity) = blockWith(EntityType.CHEST_MINECART)
        val cart = entity as StorageMinecart

        val process = processFor(ItemStack(Material.DIRT, 48))
        assertEquals(DespawnIntoResult.FULLY, strategy.despawnInto(process, block))
        assertTrue(cart.inventory.contains(Material.DIRT, 48))
    }

    @Test
    fun `a full storage minecart reports PARTIALLY with the leftover`() {
        val (block, entity) = blockWith(EntityType.CHEST_MINECART)
        val cart = entity as StorageMinecart
        for (slot in 0 until cart.inventory.size) {
            cart.inventory.setItem(slot, ItemStack(Material.STONE, 64))
        }

        val process = processFor(ItemStack(Material.DIRT, 7))
        assertEquals(DespawnIntoResult.PARTIALLY, strategy.despawnInto(process, block))
        assertEquals(7, process.item?.amount)
    }

    @Test
    fun `a furnace minecart takes fuel and refuses non-fuel`() {
        val (block, entity) = blockWith(EntityType.FURNACE_MINECART)
        val cart = entity as org.bukkit.entity.minecart.PoweredMinecart

        val process = processFor(ItemStack(Material.COAL, 2))
        assertEquals(DespawnIntoResult.PARTIALLY, strategy.despawnInto(process, block))
        assertEquals(3600, cart.fuel, "one coal adds 3600 fuel ticks")
        assertEquals(1, process.item?.amount)

        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(processFor(ItemStack(Material.DIRT)), block))

        strategy.removeFrom(Material.COAL, block)
        assertEquals(0, cart.fuel, "fuel purge zeroes the tank")
    }

    @Test
    fun `a block with no or two entities is NONE at despawn time`() {
        nextX += 8
        val emptyBlock = world.getBlockAt(nextX, 64, 0)
        assertEquals(DespawnIntoResult.NONE, strategy.despawnInto(processFor(ItemStack(Material.DIRT)), emptyBlock))
    }

    @Test
    fun `removeFrom clears the matching slot or frame`() {
        val (frameBlock, frameEntity) = blockWith(EntityType.ITEM_FRAME)
        (frameEntity as ItemFrame).setItem(ItemStack(Material.DIRT))
        strategy.removeFrom(Material.DIRT, frameBlock)
        assertTrue(frameEntity.item.type.isAir)

        val (zombieBlock, zombieEntity) = blockWith(EntityType.ZOMBIE)
        val equipment = (zombieEntity as LivingEntity).equipment!!
        equipment.setHelmet(ItemStack(Material.DIRT))
        strategy.removeFrom(ItemStack(Material.DIRT), zombieBlock)
        assertTrue(equipment.helmet == null || equipment.helmet!!.type.isAir)

        equipment.setChestplate(ItemStack(Material.IRON_CHESTPLATE))
        strategy.removeFrom(Material.IRON_CHESTPLATE, zombieBlock)
        assertTrue(equipment.chestplate == null || equipment.chestplate!!.type.isAir)

        equipment.setLeggings(ItemStack(Material.IRON_LEGGINGS))
        strategy.removeFrom(Material.IRON_LEGGINGS, zombieBlock)
        equipment.setBoots(ItemStack(Material.IRON_BOOTS))
        strategy.removeFrom(Material.IRON_BOOTS, zombieBlock)
        equipment.setItemInMainHand(ItemStack(Material.STICK))
        strategy.removeFrom(Material.STICK, zombieBlock)
        equipment.setItemInOffHand(ItemStack(Material.BONE))
        strategy.removeFrom(Material.BONE, zombieBlock)
        assertTrue(equipment.itemInOffHand.type.isAir)

        val (cartBlock, cartEntity) = blockWith(EntityType.CHEST_MINECART)
        (cartEntity as StorageMinecart).inventory.addItem(ItemStack(Material.DIRT, 10))
        strategy.removeFrom(Material.DIRT, cartBlock)
        assertFalse(cartEntity.inventory.contains(Material.DIRT))

        // No entities → silent return.
        nextX += 8
        strategy.removeFrom(Material.DIRT, world.getBlockAt(nextX, 64, 0))
    }
}
