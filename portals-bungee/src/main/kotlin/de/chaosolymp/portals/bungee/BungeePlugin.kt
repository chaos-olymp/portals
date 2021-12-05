package de.chaosolymp.portals.bungee

import com.google.gson.Gson
import de.chaosolymp.portals.bungee.command.PortalCommand
import de.chaosolymp.portals.bungee.config.CacheConfiguration
import de.chaosolymp.portals.bungee.config.DatabaseConfiguration
import de.chaosolymp.portals.bungee.config.MessageConfiguration
import de.chaosolymp.portals.bungee.listener.PluginMessageListener
import de.chaosolymp.portals.core.DatabaseService
import de.chaosolymp.portals.core.infrastructure.HikariDatabaseProvider
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File

class BungeePlugin: Plugin() {
    lateinit var portalManager: PortalManager
    lateinit var messageConfiguration: MessageConfiguration
    lateinit var pluginMessageListener: PluginMessageListener

    internal lateinit var portalCache: PortalCache
    internal lateinit var exceptionHandler: ExceptionHandler
    internal lateinit var databaseConfiguration: DatabaseConfiguration
    internal lateinit var cachingConfiguration: CacheConfiguration

    private lateinit var databaseService: DatabaseService

    override fun onEnable() {
        val startTime = System.currentTimeMillis()

        if(!dataFolder.exists()) {
            dataFolder.mkdir()
            logger.info("Created plugin data folder ${dataFolder.name}")
        }

        initializeMessageConfig()
        initializeDatabaseConfig()
        initializeCachingConfig()

        exceptionHandler = ExceptionHandler(this)
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
        logger.info("Initialized global exception handler")

        portalCache = PortalCache(this)
        logger.info("Initialized cache")

        portalManager = PortalManager(this, databaseService)
        logger.info("Initialized portal manager")

        portalManager.createTable()

        pluginMessageListener = PluginMessageListener(this)
        proxy.pluginManager.registerListener(this, this.pluginMessageListener)
        logger.info("Registered plugin message listener")

        proxy.pluginManager.registerCommand(this, PortalCommand(this))
        logger.info("Registered portal command")

        logger.info("Plugin warmup finished (Took ${System.currentTimeMillis() - startTime}ms).")
    }

    private fun initializeMessageConfig() {
        val provider = ConfigurationProvider.getProvider(YamlConfiguration::class.java)
        val file = File(this.dataFolder, "messages.yml")
        if(file.exists()) {
            val yamlConfig = provider.load(file)
            messageConfiguration = MessageConfiguration(yamlConfig)
            logger.info("Loaded configuration file ${file.name}")
        } else {
            if(file.createNewFile()) {
                val defaults = MessageConfiguration.getDefaultConfiguration()
                provider.save(defaults, file)
                messageConfiguration = MessageConfiguration(defaults)
                logger.info("Created default configuration file ${file.name}")
            }
        }
    }

    private fun initializeCachingConfig() {
        val gson = Gson()
        val file = File(dataFolder, "caching.json")
        if(file.exists()) {
            cachingConfiguration = gson.fromJson(file.readText(), CacheConfiguration::class.java)
            logger.info("Loaded configuration file ${file.name}")
        } else {
            if(file.createNewFile()) {
                val default = CacheConfiguration.getDefaultConfiguration()
                cachingConfiguration = default
                file.writeText(gson.toJson(cachingConfiguration))
                logger.info("Created default configuration file ${file.name}")
            }
        }
    }

    private fun initializeDatabaseConfig() {
        val provider = ConfigurationProvider.getProvider(YamlConfiguration::class.java)
        val file = File(dataFolder, "database.yml")
        if(file.exists()) {
            val yamlConfig = provider.load(file)
            if(yamlConfig.contains("jdbc") && yamlConfig.contains("username") && yamlConfig.contains("password")) {
                databaseConfiguration = yamlConfig.getString("jdbc")?.let {
                    DatabaseConfiguration(
                        it,
                        yamlConfig.getString("username")!!,
                        yamlConfig.getString("password")!!
                    )
                }!!
            } else {
                logger.severe("Error whilst loading configuration file")
            }
            this.logger.info("Loaded configuration file ${file.name}")
        } else {
            if(file.createNewFile()) {
                val defaultConfig = Configuration()
                val defaultJdbc = "jdbc:mysql://localhost:3306/portals"
                val defaultUsername = "root"
                val defaultPassword = "password"
                defaultConfig.set("jdbc", defaultJdbc)
                defaultConfig.set("username", defaultUsername)
                defaultConfig.set("password", defaultPassword)
                provider.save(defaultConfig, file)
                databaseConfiguration = DatabaseConfiguration(defaultJdbc, defaultUsername, defaultPassword)
                logger.info("Created default configuration file ${file.name}")
                logger.warning("Please edit your database settings - Password \"password\" is not secure enough.")
            }

        }

        databaseService = DatabaseService(HikariDatabaseProvider(databaseConfiguration.dataSource)) { sql ->
            DebugMessenger.verbose("SQL Statement Preparation", sql)
        }
    }
}