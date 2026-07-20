package com.popupmc.despawneditems.location

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Roundtrip + resilience tests for [YamlLocationRepository]. Uses a JUnit temp
 * directory — `YamlConfiguration` reads/writes plain string lists without a server.
 */
class YamlLocationRepositoryTest {

    private val logger: Logger = Logger.getLogger("YamlLocationRepositoryTest")
    private val alice: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val bob: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000b2")

    @Test
    fun `save then load roundtrips across owners and worlds`(@TempDir dir: File) {
        val repo = YamlLocationRepository(dir, logger)
        val locs = listOf(
            DespawnLocation("world", 1, 2, 3, alice),
            DespawnLocation("world", 4, 5, 6, alice),
            DespawnLocation("world_nether", 7, 8, 9, bob),
        )
        val byOwner = locs.groupBy { it.owner }
        repo.saveOwners(listOf(alice, bob)) { byOwner[it].orEmpty() }

        assertEquals(locs.toSet(), repo.loadAll().toSet())
    }

    @Test
    fun `saving an owner with no locations deletes their file`(@TempDir dir: File) {
        val repo = YamlLocationRepository(dir, logger)
        repo.saveOwners(listOf(alice)) { listOf(DespawnLocation("world", 1, 1, 1, alice)) }
        val file = File(File(dir, "userdata"), "$alice.yml")
        assertTrue(file.exists())

        repo.saveOwners(listOf(alice)) { emptyList() }
        assertFalse(file.exists(), "an owner left with no locations should have their file removed")
    }

    @Test
    fun `loadAll skips malformed lines and non-uuid files`(@TempDir dir: File) {
        val userdata = File(dir, "userdata").apply { mkdirs() }
        File(userdata, "$alice.yml").writeText("locations:\n- '1;2;3;world'\n- 'garbage'\n- 'x;y;z;world'\n")
        File(userdata, "not-a-uuid.yml").writeText("locations:\n- '1;2;3;world'\n")

        assertEquals(listOf(DespawnLocation("world", 1, 2, 3, alice)), YamlLocationRepository(dir, logger).loadAll())
    }

    @Test
    fun `loadAll on a missing directory is empty`(@TempDir dir: File) {
        assertTrue(YamlLocationRepository(File(dir, "does-not-exist"), logger).loadAll().isEmpty())
    }

    @Test
    fun `incremental save only rewrites the given owners`(@TempDir dir: File) {
        val repo = YamlLocationRepository(dir, logger)
        repo.saveOwners(listOf(alice, bob)) { listOf(DespawnLocation("world", 1, 1, 1, it)) }
        val bobFile = File(File(dir, "userdata"), "$bob.yml")
        val bobModifiedBefore = bobFile.lastModified()

        Thread.sleep(10)
        repo.saveOwners(listOf(alice)) { listOf(DespawnLocation("world", 2, 2, 2, it)) }
        assertEquals(bobModifiedBefore, bobFile.lastModified(), "bob's file should not be rewritten")
    }
}
