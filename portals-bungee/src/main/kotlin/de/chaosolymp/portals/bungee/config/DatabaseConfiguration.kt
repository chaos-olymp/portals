package de.chaosolymp.portals.bungee.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource


data class DatabaseConfiguration(val jdbcUrl: String, val userName: String, val password: String) {
    private val config: HikariConfig = HikariConfig()
    val dataSource: HikariDataSource

    init {
        config.jdbcUrl = jdbcUrl
        config.username = userName
        config.password = password
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        dataSource = HikariDataSource(config)
    }

}