package de.chaosolymp.portals.bukkit

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import de.chaosolymp.portals.bukkit.extensions.sendPluginMessage
import de.chaosolymp.portals.bukkit.listener.PluginCommunicationListener
import de.chaosolymp.portals.bukkit.listener.PortalListener
import de.chaosolymp.portals.core.messages.server_to_proxy.AuthorizeTeleportRequestPluginMessage
import de.chaosolymp.portals.core.messages.server_to_proxy.ValidationPluginMessage
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture

class BukkitPlugin: JavaPlugin {

    private var worldGuard: WorldGuard? = null

    private lateinit var configFile: File
    private lateinit var config: YamlConfiguration
    private lateinit var pluginCommunicationListener: PluginCommunicationListener

    internal val portalRequestMap = mutableMapOf<Pair<String, Triple<Int, Int, Int>>, CompletableFuture<Boolean>>()
    internal val pendingTeleports = mutableListOf<Pair<UUID, Location>>()

    // Called via reflection in testing environment by MockBukkit
    @Suppress("unused")
    constructor() : super()

    // Called via reflection in testing environment by MockBukkit
    @Suppress("unused")
    private constructor(loader: JavaPluginLoader,
                          description: PluginDescriptionFile,
                          dataFolder: File,
                          file: File) : super(loader, description, dataFolder, file)

    override fun onEnable() {
        if(!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        pluginCommunicationListener = PluginCommunicationListener(this)
        initConfig()
        server.pluginManager.registerEvents(PortalListener(this),this)

        logger.info("Initialized global exception handler")

        if (RuntimeStatics.TEST_ENVIRONMENT) {
            logger.info("Skipping plugin channel listener registration because we're in TEST_ENVIRONMENT")
        } else {
            logger.info("Environment: Production")
            server.messenger.registerIncomingPluginChannel(this, "BungeeCord", this.pluginCommunicationListener)
            server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
            logger.info("Registered Plugin Channel listeners")
        }

        if(server.pluginManager.isPluginEnabled("WorldGuard")) {
            worldGuard = WorldGuard.getInstance()
        }
    }

    private fun initConfig() {
        configFile = File(dataFolder, "messages.yml")
        if(!configFile.exists()) {
            configFile.createNewFile()
            config = YamlConfiguration()
            config.addDefault("messages.sneak-to-tp", "> Schleiche um dich zu teleportieren <")
            config.options().copyDefaults(true)
            config.save(configFile)
            logger.info("Default configuration ${configFile.name} created!")
        } else {
            config = YamlConfiguration.loadConfiguration(configFile)
            logger.info("Config ${configFile.name} successfully loaded!")
        }
    }

    internal fun handlePortalAppearance(player: Player) {
        val builder = ComponentBuilder(config.getString("messages.sneak-to-tp"))
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *builder.create())
    }

    @Suppress("UnstableApiUsage")
    internal fun teleport(player: Player, block: Block) {
        val world = block.location.world!!.name
        val x = block.location.blockX
        val y = block.location.blockY
        val z = block.location.blockZ

        val outgoingMessage = AuthorizeTeleportRequestPluginMessage(player.uniqueId, world, x + 1, y - 1, z)
        player.sendPluginMessage(this, outgoingMessage)
        logger.info("Wrote outgoing message: $outgoingMessage")
    }

    internal fun canCreatePortal(player: Player): Boolean {
        val blockLocation = player.location
        val material = player.location.world!!.getBlockAt(Location(blockLocation.world,
            blockLocation.blockX.toDouble(),
            // no -1 because END_PORTAL_FRAME is 0.8 blocks height
            blockLocation.blockY.toDouble(),
            blockLocation.blockZ.toDouble()
        )).type

        return material == PORTAL_BASE_MATERIAL && !isInSpawnRadius(player) && hasRegionPermissions(player)
    }

    fun isInSpawnRadius(player: Player): Boolean {
        if(player.isOp) { // op's can make everything in spawn radius
            return false
        }
        val location = player.location
        val world = location.world!!
        val spawnLocation = world.spawnLocation

        return spawnLocation.distance(location) < this.server.spawnRadius // player is in spawn radius
    }

    private fun hasRegionPermissions(player: Player): Boolean {
        if(worldGuard == null) return true
        val location = player.location

        val regionContainer = worldGuard!!.platform.regionContainer
        val worldGuardWorld = BukkitAdapter.adapt(location.world)
        val regionManager = regionContainer[worldGuardWorld]!!

        return regionManager.getApplicableRegions(BlockVector3.at(location.blockX, location.blockY, location.blockZ))
                // Region owner is required to create portal
            ?.isOwnerOfAll(WorldGuardPlugin.inst().wrapPlayer(player))!!
    }

    fun isValidPortal(player: Player, location: Location): Boolean {
        val world = location.world!!.name
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ

        val future = CompletableFuture<Boolean>()
        portalRequestMap[Pair(world, Triple(x, y, z))] = future

        val outgoingMessage = ValidationPluginMessage(player.uniqueId, world, x, y, z)
        player.sendPluginMessage(this, outgoingMessage)
        logger.info("Wrote outgoing message: $outgoingMessage")

        return future.get()
    }
}