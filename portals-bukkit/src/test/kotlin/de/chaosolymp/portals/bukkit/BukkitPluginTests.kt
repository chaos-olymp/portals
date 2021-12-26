package de.chaosolymp.portals.bukkit

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import org.bukkit.Location
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BukkitPluginTests {

    private lateinit var server: ServerMock
    private lateinit var plugin: BukkitPlugin

    @BeforeAll
    fun setup() {
        RuntimeStatics.TEST_ENVIRONMENT = true

        server = MockBukkit.mock()
        plugin = MockBukkit.load(BukkitPlugin::class.java)
    }

    @Test
    fun testPortalAppearanceHandling() {
        val player = server.addPlayer()
        plugin.handlePortalAppearance(player)
    }

    @Test
    fun testIsInSpawnRadiusOp() {
        val player = server.addPlayer()
        player.isOp = true

        val inSpawnRadius = plugin.isInSpawnRadius(player)
        Assertions.assertFalse(inSpawnRadius)
    }

    @Test
    fun testIsInSpawnRadiusNonOp() {
        val world = server.addSimpleWorld("TestWorld")
        world.spawnLocation = Location(world, 0.0, 0.0, 0.0)

        val player = server.addPlayer()
        player.location = Location(world, 0.0, 0.0, 0.0);

        val inSpawnRadius = plugin.isInSpawnRadius(player);
        Assertions.assertTrue(inSpawnRadius)
    }

    @AfterAll
    fun tearDown() {
        MockBukkit.unmock()
    }

}