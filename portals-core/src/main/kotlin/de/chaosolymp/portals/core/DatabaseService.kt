package de.chaosolymp.portals.core

import de.chaosolymp.portals.core.extension.getUUID
import de.chaosolymp.portals.core.extension.prepareAndLogStatement
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.util.*

class DatabaseService(
    private val databaseProvider: DatabaseProvider,
    private val callback: PrepareStatementCallback? = null
) {

    private val regex = Regex("^[a-z_]+")

    fun createTable() {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
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
                    	PRIMARY KEY (`id`, `name`)
                    )
                """.trimIndent()
                )
            stmt.execute()
        }
    }

    fun isNameValid(name: String) = name.isNotEmpty() && name.length < 33 && name.matches(regex)

    fun doesNameExist(name: String): Boolean {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT COUNT(name) 
                FROM `portals` 
                WHERE name = ?
            """.trimIndent()
                )
            stmt.setString(1, name)
            val rs = stmt.executeQuery()
            return if (rs.next()) {
                rs.getInt(1) > 0
            } else {
                false
            }
        }
    }

    fun createPortal(
        owner: UUID,
        name: String,
        server: String,
        public: Boolean,
        world: String,
        x: Int,
        y: Int,
        z: Int
    ): Int? {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                INSERT INTO `portals` (owner, name, public, created, server, world, x, y, z) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
                    Statement.RETURN_GENERATED_KEYS
                )
            stmt.setBytes(1, UUIDUtils.getBytesFromUUID(owner))
            stmt.setString(2, name)
            stmt.setBoolean(3, public)
            stmt.setTimestamp(4, Timestamp.from(Instant.now()))
            stmt.setString(5, server)
            stmt.setString(6, world)
            stmt.setInt(7, x)
            stmt.setInt(8, y)
            stmt.setInt(9, z)
            val affectedRows = stmt.executeUpdate()
            return if (affectedRows == 0) {
                null
            } else {
                val generatedKeys = stmt.generatedKeys
                if (generatedKeys.next()) {
                    generatedKeys.getInt(1)
                } else {
                    null
                }
            }
        }
    }

    fun setPublic(id: Int, public: Boolean) {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                UPDATE `portals` 
                SET public = ? 
                WHERE id = ?
            """.trimIndent()
                )
            stmt.setBoolean(1, public)
            stmt.setInt(2, id)
            stmt.execute()
        }
    }

    fun setPublic(name: String, public: Boolean) {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                UPDATE `portals` 
                SET public = ? 
                WHERE name = ?
            """.trimIndent()
                )
            stmt.setBoolean(1, public)
            stmt.setString(2, name)
            stmt.execute()
        }
    }

    fun getPortal(id: Int): Portal? {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT 
                    owner, 
                    name, 
                    display_name, 
                    public, 
                    created, 
                    updated, 
                    server, 
                    world, 
                    x, 
                    y, 
                    z, 
                    link 
                FROM `portals` 
                WHERE id = ?
            """.trimIndent()
                )
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()

            if (rs.next()) {
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
                var link: Int? = rs.getInt("link")
                if (rs.wasNull()) {
                    link = null
                }

                return Portal(
                    id,
                    uuid,
                    name,
                    displayName,
                    public,
                    created,
                    updated,
                    server,
                    world,
                    x,
                    y,
                    z,
                    link
                )
            } else {
                return null
            }
        }
    }

    fun getPortal(name: String): Portal? {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT 
                    id, 
                    owner, 
                    display_name, 
                    public, 
                    created, 
                    updated, 
                    server, 
                    world, 
                    x, 
                    y, 
                    z, 
                    link 
                FROM `portals` 
                WHERE name = ?
            """.trimIndent()
                )
            stmt.setString(1, name)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val id = rs.getInt("id")
                val uuid = rs.getUUID("owner")
                val displayName = rs.getString("display_name")
                val public = rs.getBoolean("public")
                val created = rs.getTimestamp("created")
                val updated = rs.getTimestamp("updated")
                val server = rs.getString("server")
                val world = rs.getString("world")
                val x = rs.getInt("x")
                val y = rs.getInt("y")
                val z = rs.getInt("z")
                var link: Int? = rs.getInt("link")
                if (rs.wasNull()) {
                    link = null
                }

                return Portal(
                    id,
                    uuid,
                    name,
                    displayName,
                    public,
                    created,
                    updated,
                    server,
                    world,
                    x,
                    y,
                    z,
                    link
                )
            } else {
                return null
            }
        }
    }

    fun remove(id: Int) {
        databaseProvider.useConnection {
            val unlinkStmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                UPDATE `portals` 
                SET `link` = NULL 
                WHERE link = ?
            """.trimIndent()
                )
            unlinkStmt.setInt(1, id)
            unlinkStmt.execute()

            val deleteStmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                DELETE FROM `portals` 
                WHERE id = ?
            """.trimIndent()
                )
            deleteStmt.setInt(1, id)
            deleteStmt.execute()
        }
    }

    fun remove(name: String) {
        databaseProvider.useConnection {
            val unlinkStmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                UPDATE `portals` 
                SET `link` = NULL 
                WHERE link = (SELECT id FROM (
                                                SELECT * 
                                                FROM portals 
                                                WHERE name = ?
                                             ) AS portals_table WHERE name = ? LIMIT 1
                             )
            """.trimIndent()
                )
            unlinkStmt.setString(1, name)
            unlinkStmt.setString(2, name)
            unlinkStmt.execute()

            val deleteStmt =
                it.prepareStatement(
                    """
                DELETE FROM `portals` 
                WHERE name = ?
            """.trimIndent()
                )
            deleteStmt.setString(1, name)
            deleteStmt.execute()
        }
    }

    fun getIdOfName(name: String): Int {
        databaseProvider.useConnection {
            val query =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT id 
                FROM `portals` 
                WHERE name = ?
            """.trimIndent()
                )
            query.setString(1, name)
            val rs = query.executeQuery()
            rs.next()
            return rs.getInt(1)
        }
    }

    fun link(originId: Int, linkId: Int) {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                UPDATE `portals` 
                SET link = ? 
                WHERE id = ?
            """.trimIndent()
                )
            stmt.setInt(1, linkId)
            stmt.setInt(2, originId)
            stmt.execute()
        }
    }

    fun rename(id: Int, name: String) {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                UPDATE `portals` 
                SET name = ? 
                WHERE id = ?
            """.trimIndent()
                )
            stmt.setString(1, name)
            stmt.setInt(2, id)
            stmt.execute()
        }
    }

    fun setDisplayName(id: Int, name: String) {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                UPDATE `portals` 
                SET display_name = ? 
                WHERE id = ?
            """.trimIndent()
                )
            stmt.setString(1, name)
            stmt.setInt(2, id)
            stmt.execute()
        }
    }

    fun isPublic(id: Int): Boolean {
        databaseProvider.useConnection {
            val query =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT `public` 
                FROM `portals` 
                WHERE id = ?
            """.trimIndent()
                )
            query.setInt(1, id)
            val rs = query.executeQuery()
            rs.next()
            return rs.getBoolean(1)
        }
    }

    fun doesPlayerOwnPortal(player: UUID, id: Int): Boolean {
        databaseProvider.useConnection {
            val query =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT COUNT(*) 
                FROM `portals` 
                WHERE owner = ? 
                AND id = ?
            """.trimIndent()
                )
            query.setBytes(1, UUIDUtils.getBytesFromUUID(player))
            query.setInt(2, id)
            val rs = query.executeQuery()
            rs.next()
            return rs.getBoolean(1)
        }
    }

    fun getNameOfId(id: Int): String {
        databaseProvider.useConnection {
            val query =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT name 
                FROM `portals` 
                WHERE id = ?
            """.trimIndent()
                )
            query.setInt(1, id)
            val rs = query.executeQuery()
            rs.next()
            return rs.getString(1)
        }
    }

    fun countPortals(): Int {
        databaseProvider.useConnection {
            val query =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT COUNT(*)
                FROM `portals`
            """.trimIndent()
                )
            val rs = query.executeQuery()
            rs.next()
            return rs.getInt(1)
        }
    }

    fun doesNameOrIdExist(nameOrId: String): Boolean {
        return if (NumberUtils.isNumber(nameOrId)) {
            this.doesIdExists(nameOrId.toInt())
        } else {
            this.doesNameExist(nameOrId)
        }
    }

    fun getDbTime(): Instant {
        databaseProvider.useConnection {
            val stmt = it.prepareAndLogStatement(callback, "SELECT UNIX_TIMESTAMP()")

            val rs = stmt.executeQuery()
            return if (rs.next()) {
                val timestamp = rs.getLong(1)
                Instant.ofEpochSecond(timestamp)
            } else {
                Instant.MIN
            }
        }
    }

    fun doesIdExists(id: Int): Boolean {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT COUNT(*) 
                FROM `portals` 
                WHERE id = ?
            """.trimIndent()
                )
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            return if (rs.next()) {
                rs.getInt(1) > 0
            } else {
                false
            }
        }
    }

    fun getPortals(
        sender: UUID?,
        listType: PortalListType,
        skip: Int,
        count: Int
    ): Iterable<Portal> {
        val stmt =
            if (sender != null && listType == PortalListType.OWN) {
                databaseProvider.useConnection {
                    val stmt =
                        it.prepareAndLogStatement(
                            callback,
                            """
                    SELECT 
                        id, 
                        name, 
                        owner, 
                        display_name, 
                        public, 
                        created, 
                        updated, 
                        server, 
                        world, 
                        x, 
                        y, 
                        z, 
                        link 
                    FROM `portals` 
                    WHERE owner = ?
                    LIMIT ?, ? 
                """.trimIndent()
                        )
                    stmt.setInt(1, skip)
                    stmt.setInt(2, skip + count)
                    stmt.setBytes(3, UUIDUtils.getBytesFromUUID(sender))
                    return@useConnection stmt
                }
            } else if (listType == PortalListType.PUBLIC) {
                databaseProvider.useConnection {
                    val stmt =
                        it.prepareAndLogStatement(
                            callback,
                            """
                    SELECT 
                        id, 
                        name, 
                        owner, 
                        display_name, 
                        public, 
                        created, 
                        updated, 
                        server, 
                        world, 
                        x, 
                        y, 
                        z, 
                        link 
                    FROM `portals` 
                    WHERE public = TRUE
                    LIMIT ?, ? 
                """.trimIndent()
                        )
                    stmt.setInt(1, skip)
                    stmt.setInt(2, skip + count)
                    return@useConnection stmt
                }
            } else {
                databaseProvider.useConnection {
                    val stmt =
                        it.prepareAndLogStatement(
                            callback,
                            """
                    SELECT 
                        id, 
                        name, 
                        owner, 
                        display_name, 
                        public, 
                        created, 
                        updated, 
                        server, 
                        world, 
                        x, 
                        y, 
                        z, 
                        link 
                    FROM `portals` 
                    LIMIT ?, ?
                """.trimIndent()
                        )
                    stmt.setInt(1, skip)
                    stmt.setInt(2, skip + count)
                    return@useConnection stmt
                }
            }
        val list = mutableListOf<Portal>()
        val rs = stmt.executeQuery()
        while (rs.next()) {
            val id = rs.getInt("id")
            val name = rs.getString("name")
            val uuid = rs.getUUID("owner")
            val displayName = rs.getString("display_name")
            val public = rs.getBoolean("public")
            val created = rs.getTimestamp("created")
            val updated = rs.getTimestamp("updated")
            val server = rs.getString("server")
            val world = rs.getString("world")
            val x = rs.getInt("x")
            val y = rs.getInt("y")
            val z = rs.getInt("z")
            var link: Int? = rs.getInt("link")
            if (rs.wasNull()) {
                link = null
            }

            list.add(
                Portal(
                    id,
                    uuid,
                    name,
                    displayName,
                    public,
                    created,
                    updated,
                    server,
                    world,
                    x,
                    y,
                    z,
                    link
                )
            )
        }

        return list
    }

    fun getPortalIdAt(server: String, world: String, x: Int, y: Int, z: Int): Int? {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT id 
                FROM `portals` 
                WHERE server = ? 
                    AND world = ? 
                    AND x = ? 
                    AND y = ? 
                    AND z = ?
            """.trimIndent()
                )
            stmt.setString(1, server)
            stmt.setString(2, world)
            stmt.setInt(3, x)
            stmt.setInt(4, y)
            stmt.setInt(5, z)

            val rs = stmt.executeQuery()

            return if (rs.next()) {
                rs.getInt("id")
            } else {
                null
            }
        }
    }

    fun getPortalLink(id: Int): Int? {
        databaseProvider.useConnection {
            val stmt =
                it.prepareAndLogStatement(
                    callback,
                    """
                SELECT link 
                FROM `portals` 
                WHERE id = ?
            """.trimIndent()
                )
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            rs.next()

            var i: Int? = rs.getInt("link")
            if (rs.wasNull()) {
                i = null
            }
            return i
        }
    }
}
