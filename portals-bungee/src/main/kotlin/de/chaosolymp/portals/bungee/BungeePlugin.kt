package de.chaosolymp.portals.bungee

import de.chaosolymp.portals.bungee.command.PortalCommand
import de.chaosolymp.portals.bungee.config.DatabaseConfiguration
import de.chaosolymp.portals.bungee.config.MessageConfiguration
import de.chaosolymp.portals.bungee.listener.PluginMessageListener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File

class BungeePlugin: Plugin() {

    lateinit var portalManager: PortalManager
    lateinit var messageConfiguration: MessageConfiguration
    lateinit var databaseConfiguration: DatabaseConfiguration
    lateinit var pluginMessageListener: PluginMessageListener

    internal lateinit var exceptionHandler: ExceptionHandler

    override fun onEnable() {
        val startTime = System.currentTimeMillis()

        if(!this.dataFolder.exists()) {
            this.dataFolder.mkdir()
            this.logger.info("Created plugin data folder ${dataFolder.name}")
        }

        this.initializeMessageConfig()
        this.initializeDatabaseConfig()

        exceptionHandler = ExceptionHandler(this)
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
        this.logger.info("Initialized global exception handler")

        this.portalManager = PortalManager(this)
        this.logger.info("Initialized portal manager")

        this.portalManager.createTable()

        this.pluginMessageListener = PluginMessageListener(this)
        this.proxy.pluginManager.registerListener(this, this.pluginMessageListener)
        this.logger.info("Registered plugin message listener")

        this.proxy.pluginManager.registerCommand(this, PortalCommand(this))
        this.logger.info("Registered portal command")

        this.logger.info("Plugin warmup finished (Took ${System.currentTimeMillis() - startTime}ms).")
    }

    private fun initializeMessageConfig() {
        val provider = ConfigurationProvider.getProvider(YamlConfiguration::class.java)
        val file = File(this.dataFolder, "messages.yml")
        if(file.exists()) {
            val yamlConfig = provider.load(file)
            this.messageConfiguration = MessageConfiguration(yamlConfig)
            this.logger.info("Loaded configuration file ${file.name}")
        } else {
            if(file.createNewFile()) {
                val defaults = MessageConfiguration.getDefaultConfiguration()
                provider.save(defaults, file)
                this.messageConfiguration = MessageConfiguration(defaults)
                this.logger.info("Created default configuration file ${file.name}")
            }

        }
    }

    private fun initializeDatabaseConfig() {
        val provider = ConfigurationProvider.getProvider(YamlConfiguration::class.java)
        val file = File(this.dataFolder, "database.yml")
        if(file.exists()) {
            val yamlConfig = provider.load(file)
            if(yamlConfig.contains("jdbc") && yamlConfig.contains("username") && yamlConfig.contains("password")) {
                this.databaseConfiguration = yamlConfig.getString("jdbc")?.let {
                    DatabaseConfiguration(
                        it,
                        yamlConfig.getString("username")!!,
                        yamlConfig.getString("password")!!
                    )
                }!!
            } else {
                this.logger.severe("Error whilst loading configuration file")
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
                this.databaseConfiguration = DatabaseConfiguration(defaultJdbc, defaultUsername, defaultPassword)
                this.logger.info("Created default configuration file ${file.name}")
                this.logger.warning("Please edit your database settings - Password \"password\" is not secure enough.")
            }

        }
    }
}