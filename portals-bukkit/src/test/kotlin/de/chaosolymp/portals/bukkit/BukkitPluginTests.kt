package de.chaosolymp.portals.bukkit

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BukkitPluginTests {

    private lateinit var server: ServerMock;
    private lateinit var plugin: BukkitPlugin

    @BeforeAll
    fun setup() {
        RuntimeStatics.TEST_ENVIRONMENT = true;

        server = MockBukkit.mock();
        plugin = MockBukkit.load(BukkitPlugin::class.java)
    }

    @Test
    fun test() {

    }

    @AfterAll
    fun tearDown() {
        MockBukkit.unmock();
    }

}