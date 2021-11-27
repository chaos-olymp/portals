package de.chaosolymp.portals.core.infrastructure

import com.zaxxer.hikari.HikariDataSource
import de.chaosolymp.portals.core.DatabaseProvider
import java.sql.Connection

class HikariDatabaseProvider(private val hikariDataSource: HikariDataSource) : DatabaseProvider {
    override val connection: Connection
        get() = hikariDataSource.connection
}