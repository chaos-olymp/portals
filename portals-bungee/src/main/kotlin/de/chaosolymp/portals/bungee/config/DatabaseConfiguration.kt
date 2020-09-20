package de.chaosolymp.portals.bungee.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource


data class DatabaseConfiguration(val jdbcUrl: String, val userName: String, val password: String) {
    private val config: HikariConfig = HikariConfig()
    val dataSource: HikariDataSource

    init {
        this.config.jdbcUrl = jdbcUrl
        this.config.username = userName
        this.config.password = password
        this.config.addDataSourceProperty("cachePrepStmts", "true")
        this.config.addDataSourceProperty("prepStmtCacheSize", "250")
        this.config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        this.dataSource = HikariDataSource(this.config)
    }

}