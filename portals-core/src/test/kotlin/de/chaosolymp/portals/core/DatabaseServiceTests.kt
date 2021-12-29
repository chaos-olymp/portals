package de.chaosolymp.portals.core

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.chaosolymp.portals.core.infrastructure.HikariDatabaseProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.Logger
import org.junit.platform.commons.logging.LoggerFactory
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DatabaseServiceTests {
    private val logger: Logger = LoggerFactory.getLogger(DatabaseServiceTests::class.java)

    private lateinit var hikariConfig: HikariConfig
    private lateinit var hikariDataSource: HikariDataSource
    private lateinit var databaseProvider: DatabaseProvider
    private lateinit var databaseService: DatabaseService
    private lateinit var container: MySQLContainer<*>

    @BeforeEach
    fun setup() {
        container = MySQLContainer<Nothing>("mysql:8.0.27").apply {
            withDatabaseName("portals")
            withExposedPorts(3306)
            waitingFor(Wait.forLogMessage("ready for connections.", 1))
        }

        container.start()

        val mappedPort = container.getMappedPort(3306)

        hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = "jdbc:mysql://${container.host}:$mappedPort/portals"
        hikariConfig.username = container.username
        hikariConfig.password = container.password
        hikariConfig.driverClassName = "com.mysql.cj.jdbc.Driver"
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

        hikariDataSource = HikariDataSource(hikariConfig)
        databaseProvider = HikariDatabaseProvider(hikariDataSource)
        logger.info { "Database initialized" }

        databaseService = DatabaseService(databaseProvider)
        databaseService.createTable()
        logger.info { "Tables created" }

        logger.info { "Test setup done" }
    }

    @Test
    fun testConnection() {
        assertFalse(hikariDataSource.isClosed)
        assertNotNull(hikariDataSource)

        assertNotNull(databaseProvider.connection)
    }

    @Test
    fun testCountPortals() {
        val initialCount = databaseService.countPortals()
        assertEquals(0, initialCount)

        databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", true, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertEquals(1, databaseService.countPortals())
    }

    @Test
    fun testRemovePortalById() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", true, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)
        assertTrue(databaseService.doesIdExists(portalId))

        databaseService.remove(portalId)
        assertFalse(databaseService.doesIdExists(portalId))
    }

    @Test
    fun testRemovePortalByName() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", true, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)
        assertTrue(databaseService.doesIdExists(portalId))

        val name = databaseService.getNameOfId(portalId)
        assertTrue(databaseService.doesNameExist(name))

        databaseService.remove(name)
        assertFalse(databaseService.doesIdExists(portalId))
    }

    @Test
    fun testSetPublicById() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)
        assertTrue(databaseService.doesIdExists(portalId))

        databaseService.setPublic(portalId, true)
        val portal = databaseService.getPortal(portalId)

        assertNotNull(portal)
        assertTrue(portal.public)
    }

    @Test
    fun testSetPublicByName() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)
        assertTrue(databaseService.doesIdExists(portalId))

        val name = databaseService.getNameOfId(portalId)
        assertTrue(databaseService.doesNameExist(name))

        databaseService.setPublic(name, true)
        val portal = databaseService.getPortal(name)
        val isPublic = databaseService.isPublic(portalId)

        assertNotNull(portal)
        assertTrue(portal.public)
        assertTrue(isPublic)
    }

    @Test
    fun testLinkage() {
        val originPortalId = databaseService.createPortal(UUID.randomUUID(), "first_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(originPortalId)

        val targetPortalId = databaseService.createPortal(UUID.randomUUID(), "second_portal", "test_server", false, "test_world", 20, 20, 30, 0.0F, 0.0F)
        assertNotNull(targetPortalId)

        val initialLinkOfOriginPortalId = databaseService.getPortalLink(originPortalId)
        assertNull(initialLinkOfOriginPortalId)

        databaseService.link(originPortalId, targetPortalId)
        val linkOfOriginPortalId = databaseService.getPortalLink(originPortalId)
        assertNotNull(linkOfOriginPortalId)

        assertEquals(targetPortalId, linkOfOriginPortalId)
    }

    @Test
    fun testRename() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)

        val initialName = databaseService.getNameOfId(portalId)
        assertEquals("test_portal", initialName)

        databaseService.rename(portalId, "renamed_portal")
        val renamedName = databaseService.getNameOfId(portalId)
        assertEquals("renamed_portal", renamedName)
    }

    @Test
    fun testGetPortalIdAt() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)

        val obtainedPortalId = databaseService.getPortalIdAt("test_server", "test_world", 10, 20, 30)
        assertNotNull(obtainedPortalId)
        assertEquals(portalId, obtainedPortalId)
    }


    @Test
    fun testDoesPlayerOwnPortalTrue() {
        val uuid = UUID.randomUUID()
        val portalId = databaseService.createPortal(uuid, "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)

        assertTrue(databaseService.doesPlayerOwnPortal(uuid, portalId))
    }

    @Test
    fun testDoesPlayerOwnPortalFalse() {
        val uuid = UUID.randomUUID()
        val portalId = databaseService.createPortal(uuid, "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)

        assertFalse(databaseService.doesPlayerOwnPortal(UUID.randomUUID(), portalId))
    }

    @Test
    fun testDoesIdExists() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)

        assertTrue(databaseService.doesIdExists(portalId))
    }

    @Test
    fun testDoesNameOrIdExists() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)

        assertTrue(databaseService.doesNameOrIdExist("test_portal"))
        assertTrue(databaseService.doesNameOrIdExist(portalId.toString()))
    }

    @Test
    fun testSetDisplayName() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)

        val initialPortal = databaseService.getPortal(portalId)
        assertNotNull(initialPortal)
        assertNull(initialPortal.displayName)

        val displayName = "Display Name with v€ry spê¢iál charàctèrs"
        databaseService.setDisplayName(portalId, displayName)

        val portal = databaseService.getPortal(portalId)
        assertNotNull(portal)
        assertEquals(displayName, portal.displayName)
    }

    @Test
    fun testGetIdOfName() {
        val portalId = databaseService.createPortal(UUID.randomUUID(), "test_portal", "test_server", false, "test_world", 10, 20, 30, 0.0F, 0.0F)
        assertNotNull(portalId)

        val obtainedId = databaseService.getIdOfName("test_portal")
        assertNotNull(obtainedId)

        assertEquals(portalId, obtainedId)
    }

    @Test
    fun testGetOffsetLimit() {
        for(i in 1 until 30) {
            databaseService.createPortal(UUID.randomUUID(), "test_portal_$i", "test_server", false, "test_world", i, 20, 30, 0.0F, 0.0F)
        }

        val firstFive = databaseService.getPortals(0, 5)
        assertEquals(5, firstFive.size)
        assertNotNull(firstFive.firstOrNull { p -> p.name == "test_portal_1" })
        assertNotNull(firstFive.firstOrNull { p -> p.name == "test_portal_2" })
        assertNotNull(firstFive.firstOrNull { p -> p.name == "test_portal_3" })
        assertNotNull(firstFive.firstOrNull { p -> p.name == "test_portal_4" })
        assertNotNull(firstFive.firstOrNull { p -> p.name == "test_portal_5" })

        val threeWithOffset = databaseService.getPortals(8, 3)
        assertEquals(3, threeWithOffset.size)
        assertNotNull(firstFive.firstOrNull { p -> p.name == "test_portal_8" })
        assertNotNull(firstFive.firstOrNull { p -> p.name == "test_portal_9" })
        assertNotNull(firstFive.firstOrNull { p -> p.name == "test_portal_10" })
    }


    @AfterEach
    fun tearDown() {
        logger.info { "Test tear down started" }
        //hikariDataSource.close()
        //container.stop()
        logger.info { "Test tear down done" }
    }

}