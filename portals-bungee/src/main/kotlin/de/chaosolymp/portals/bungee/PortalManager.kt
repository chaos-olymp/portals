package de.chaosolymp.portals.bungee

import de.chaosolymp.portals.bungee.event.PortalCreateEvent
import de.chaosolymp.portals.bungee.event.PortalRemoveEvent
import de.chaosolymp.portals.core.NumberUtils
import de.chaosolymp.portals.core.Portal
import de.chaosolymp.portals.core.UUIDUtils
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.util.*

class PortalManager(private val plugin: BungeePlugin) {

    private val regex = Regex("^[a-z_]+")

    fun createTable() {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement(
                """
                    CREATE TABLE IF NOT EXISTS `portals` (
                    	`id` INT unsigned NOT NULL AUTO_INCREMENT,
                    	`owner` BINARY(16) DEFAULT NULL,
                    	`name` VARCHAR(32) NOT NULL,
                    	`display_name` NVARCHAR(128) DEFAULT NULL,
                    	`public` BOOLEAN NOT NULL,
                    	`created` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    	`updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    	`server` VARCHAR(32) NOT NULL,
                    	`world` VARCHAR(32) NOT NULL,
                    	`x` INT NOT NULL,
                    	`y` INT NOT NULL,
                    	`z` INT NOT NULL,
                    	`link` INT unsigned DEFAULT NULL,
                    	PRIMARY KEY (`id`,`name`)
                    );
                """.trimIndent()
            )
            stmt.execute()
        }
    }

    fun isNameValid(name: String) = name.isNotEmpty() && name.length < 33 && name.matches(regex)

    fun doesNameExist(name: String): Boolean {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("SELECT COUNT(name) FROM `portals` WHERE name = ?;")
            stmt.setString(1, name)
            val rs = stmt.executeQuery()
            return if(rs.next()) {
                rs.getInt(1) > 0
            } else {
                false
            }
        }
    }
    
    fun createPortal(owner: UUID, name: String, server: String, public: Boolean, world: String, x: Int, y: Int, z: Int): Int? {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val event = PortalCreateEvent()
            plugin.proxy.pluginManager.callEvent(event)

            if(event.isCancelled) {
                return null
            }

            val stmt = it.prepareStatement("INSERT INTO `portals` (owner, name, public, created, server, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)
            stmt.setBytes(1,  UUIDUtils.getBytesFromUUID(owner))
            stmt.setString(2, name)
            stmt.setBoolean(3, public)
            stmt.setTimestamp(4, Timestamp.from(Instant.now()))
            stmt.setString(5, server)
            stmt.setString(6, world)
            stmt.setInt(7, x)
            stmt.setInt(8, y)
            stmt.setInt(9, z)
            val affectedRows = stmt.executeUpdate()
            return if(affectedRows == 0) {
                null
            } else {
                val generatedKeys = stmt.generatedKeys
                if(generatedKeys.next()) {
                    generatedKeys.getInt(1)
                } else {
                    null
                }
            }
        }
    }

    fun setPublic(id: Int, public: Boolean) {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("UPDATE `portals` SET public = ? WHERE id = ?;")
            stmt.setBoolean(1, public)
            stmt.setInt(2, id)
            stmt.execute()
        }
    }

    fun setPublic(name: String, public: Boolean) {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("UPDATE `portals` SET public = ? WHERE name = ?;")
            stmt.setBoolean(1, public)
            stmt.setString(2, name)
            stmt.execute()
        }
    }

    fun getPortal(id: Int): Portal? {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("SELECT owner, name, display_name, public, created, updated, server, world, x, y, z, link FROM `portals` WHERE id = ?;")
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()

            if(rs.next()) {
                val uuid = UUIDUtils.getUUIDFromBytes(rs.getBytes("owner"))
                val name = rs.getString("name")
                val displayName = rs.getString("display_name")
                val public = rs.getBoolean("public")
                val created = rs.getTimestamp("created")
                val updated = rs.getTimestamp("updated")
                val server = rs.getString("server")
                val world = rs.getString("world")
                val x = rs.getInt("x")
                val y = rs.getInt("y")
                val z = rs.getInt("z")
                val link: Int? = rs.getInt("link")

                return Portal(id, uuid, name, displayName, public, created, updated, server, world, x, y, z, link)
            } else {
                return null
            }
        }
    }

    fun getPortal(name: String): Portal? {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("SELECT id, owner, display_name, public, created, updated, server, world, x, y, z, link FROM `portals` WHERE name = ?;")
            stmt.setString(1, name)
            val rs = stmt.executeQuery()

            if(rs.next()) {
                val id = rs.getInt("id")
                val uuid = UUIDUtils.getUUIDFromBytes(rs.getBytes("owner"))
                val displayName = rs.getString("display_name")
                val public = rs.getBoolean("public")
                val created = rs.getTimestamp("created")
                val updated = rs.getTimestamp("updated")
                val server = rs.getString("server")
                val world = rs.getString("world")
                val x = rs.getInt("x")
                val y = rs.getInt("y")
                val z = rs.getInt("z")
                val link: Int? = rs.getInt("link")

                return Portal(id, uuid, name, displayName, public, created, updated, server, world, x, y, z, link)
            } else {
                return null
            }
        }
    }

    fun remove(id: Int) {
        val event = PortalRemoveEvent()
        plugin.proxy.pluginManager.callEvent(event)

        if(event.isCancelled) {
            return
        }

        this.plugin.databaseConfiguration.dataSource.connection.use {
            val unlinkStmt = it.prepareStatement("UPDATE `portals` SET `link` = NULL WHERE link = ?;")
            unlinkStmt.setInt(1, id)
            unlinkStmt.execute()

            val deleteStmt = it.prepareStatement("DELETE FROM `portals` WHERE id = ?;")
            deleteStmt.setInt(1, id)
            deleteStmt.execute()
        }
    }

    fun remove(name: String) {
        val event = PortalRemoveEvent()
        plugin.proxy.pluginManager.callEvent(event)

        if(event.isCancelled) {
            return
        }

        this.plugin.databaseConfiguration.dataSource.connection.use {
            val unlinkStmt = it.prepareStatement("UPDATE `portals` SET `link` = NULL WHERE link = (SELECT id FROM portals WHERE name = ? LIMIT 1);")
            unlinkStmt.setString(1, name)
            unlinkStmt.execute()

            val deleteStmt = it.prepareStatement("DELETE FROM `portals` WHERE name = ?;")
            deleteStmt.setString(1, name)
            deleteStmt.execute()
        }
    }

    fun getIdOfName(name: String): Int {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val query = it.prepareStatement("SELECT id FROM `portals` WHERE name = ?;")
            query.setString(1, name)
            val rs = query.executeQuery()
            rs.next()
            return rs.getInt(1)
        }
    }

    fun link(originId: Int, linkId: Int) {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("UPDATE `portals` SET link = ? WHERE id = ?;")
            stmt.setInt(1, linkId)
            stmt.setInt(2, originId)
            stmt.execute()
        }
    }

    fun rename(id: Int, name: String) {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("UPDATE `portals` SET name = ? WHERE id = ?;")
            stmt.setString(1, name)
            stmt.setInt(2, id)
            stmt.execute()
        }
    }

    fun setDisplayName(id: Int, name: String) {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("UPDATE `portals` SET display_name = ? WHERE id = ?;")
            stmt.setString(1, name)
            stmt.setInt(2, id)
            stmt.execute()
        }
    }

    fun isPublic(id: Int): Boolean {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val query = it.prepareStatement("SELECT `public` FROM `portals` WHERE id = ?;")
            query.setInt(1, id)
            val rs = query.executeQuery()
            rs.next()
            return rs.getBoolean(1)
        }
    }

    fun doesPlayerOwnPortal(player: UUID, id: Int): Boolean {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val query = it.prepareStatement("SELECT COUNT(*) FROM `portals` WHERE owner = ? AND id = ?;")
            query.setBytes(1, UUIDUtils.getBytesFromUUID(player))
            query.setInt(2, id)
            val rs = query.executeQuery()
            rs.next()
            return rs.getBoolean(1)
        }
    }

    fun getNameOfId(id: Int): String {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val query = it.prepareStatement("SELECT name FROM `portals` WHERE id = ?;")
            query.setInt(1, id)
            val rs = query.executeQuery()
            rs.next()
            return rs.getString(1)
        }
    }

    fun countPortals(): Int {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val query = it.prepareStatement("SELECT COUNT(*) FROM `portals`;")
            val rs = query.executeQuery()
            rs.next()
            return rs.getInt(1)
        }
    }

    fun doesNameOrIdExist(nameOrId: String): Boolean {
        return if(NumberUtils.isNumber(nameOrId)) {
            this.doesIdExists(nameOrId.toInt())
        } else {
            this.doesNameExist(nameOrId)
        }
    }

    fun doesPlayerOwnPortalOrHasOtherAccess(sender: CommandSender, id: Int): Boolean {
        if(sender.hasPermission("portals.admin")) {
            return true
        }

        if(sender is ProxiedPlayer) {
            return this.plugin.portalManager.doesPlayerOwnPortal(sender.uniqueId, id)
        }

        return false
    }

    fun doesIdExists(id: Int): Boolean {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("SELECT COUNT(*) FROM `portals` WHERE id = ?;")
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            return if(rs.next()) {
                rs.getInt(1) > 0
            } else {
                false
            }
        }
    }

    fun getPortals(sender: CommandSender, listType: PortalListType, skip: Int, count: Int): Iterable<Portal> {
        val stmt = if(sender is ProxiedPlayer && listType == PortalListType.OWN) {
            this.plugin.databaseConfiguration.dataSource.connection.use {
                val stmt = it.prepareStatement("SELECT id, name, owner, display_name, public, created, updated, server, world, x, y, z, link FROM `portals` LIMIT ?,? WHERE owner = ?;")
                stmt.setInt(1, skip)
                stmt.setInt(2, skip + count)
                stmt.setBytes(3, UUIDUtils.getBytesFromUUID(sender.uniqueId))
                return@use stmt
            }
        } else if(listType == PortalListType.PUBLIC) {
            this.plugin.databaseConfiguration.dataSource.connection.use {
                val stmt = it.prepareStatement("SELECT id, name, owner, display_name, public, created, updated, server, world, x, y, z, link FROM `portals` LIMIT ?,? WHERE public = TRUE;")
                stmt.setInt(1, skip)
                stmt.setInt(2, skip + count)
                return@use stmt
            }
        } else {
            this.plugin.databaseConfiguration.dataSource.connection.use {
                val stmt = it.prepareStatement("SELECT id, name, owner, display_name, public, created, updated, server, world, x, y, z, link FROM `portals` LIMIT ?,?")
                stmt.setInt(1, skip)
                stmt.setInt(2, skip + count)
                return@use stmt
            }
        }
        val list = mutableListOf<Portal>()
        val rs = stmt.executeQuery()
        while(rs.next()) {
            val id = rs.getInt("id")
            val name = rs.getString("name")
            val uuid = UUIDUtils.getUUIDFromBytes(rs.getBytes("owner"))
            val displayName = rs.getString("display_name")
            val public = rs.getBoolean("public")
            val created = rs.getTimestamp("created")
            val updated = rs.getTimestamp("updated")
            val server = rs.getString("server")
            val world = rs.getString("world")
            val x = rs.getInt("x")
            val y = rs.getInt("y")
            val z = rs.getInt("z")
            val link: Int? = rs.getInt("link")

            list.add(Portal(id, uuid, name, displayName, public, created, updated, server, world, x, y, z, link))
        }

        return list
    }

    fun getPortalIdAt(server: String, world: String, x: Int, y: Int, z: Int): Int? {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("SELECT id FROM `portals` WHERE server = ? AND world = ? AND x = ? AND y = ? AND z = ?;")
            stmt.setString(1, server)
            stmt.setString(2, world)
            stmt.setInt(3, x)
            stmt.setInt(4, y)
            stmt.setInt(5, z)

            val rs = stmt.executeQuery()

            return if(rs.next()) {
                rs.getInt("id")
            } else {
                null
            }
        }
    }

    fun getPortalLink(id: Int): Int {
        this.plugin.databaseConfiguration.dataSource.connection.use {
            val stmt = it.prepareStatement("SELECT link FROM `portals` WHERE id = ?;")
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            rs.next()
            return rs.getInt("link")
        }
    }
}