package de.chaosolymp.portals.bukkit

import com.google.common.io.ByteStreams
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import de.chaosolymp.portals.bukkit.listener.PluginCommunicationListener
import de.chaosolymp.portals.bukkit.listener.PortalListener
import de.chaosolymp.portals.core.IDENTIFIER_AUTHORIZE_TELEPORT
import de.chaosolymp.portals.core.UUIDUtils
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Color
import java.io.File
import java.util.*

class BukkitPlugin: JavaPlugin() {

    private var worldGuard: WorldGuard? = null

    private lateinit var configFile: File
    private lateinit var config: YamlConfiguration
    private lateinit var pluginCommunicationListener: PluginCommunicationListener

    internal val pendingTeleports = mutableListOf<Pair<UUID, Location>>()

    override fun onEnable() {
        this.pluginCommunicationListener = PluginCommunicationListener(this)
        this.initConfig()
        this.server.pluginManager.registerEvents(PortalListener(this),this)
        this.server.messenger.registerIncomingPluginChannel(this, "BungeeCord", this.pluginCommunicationListener)
        this.server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        if(this.server.pluginManager.isPluginEnabled("WorldGuard")) {
            this.worldGuard = WorldGuard.getInstance()
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
            this.logger.info("Default configuration ${configFile.name} created!")
        } else {
            config = YamlConfiguration.loadConfiguration(configFile)
            this.logger.info("Config ${configFile.name} successfully loaded!")
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
        val output = ByteStreams.newDataOutput(world.length + 54)
        val uuid = UUIDUtils.getBytesFromUUID(player.uniqueId)
        output.writeUTF(IDENTIFIER_AUTHORIZE_TELEPORT)
        output.write(uuid)
        output.writeUTF(world)
        output.writeInt(x)
        output.writeInt(y)
        output.writeInt(z)

        player.sendPluginMessage(this, "BungeeCord", output.toByteArray())
    }

    internal fun canCreatePortal(player: Player): Boolean {
        val blockLocation = player.location.subtract(0.0, 1.0, 0.0)
        val material = player.location.world!!.getBlockAt(blockLocation).type

        return material == PORTAL_BASE_MATERIAL && !isInSpawnRadius(player) && hasRegionPermissions(player)
    }

    private fun isInSpawnRadius(player: Player): Boolean {
        if(player.isOp) { // op's can make everything in spawn radius
            return false
        }
        val location = player.location
        val spawnLocation = location.world?.spawnLocation

        return spawnLocation?.distance(location)!! < this.server.spawnRadius // player is in spawn radius
    }

    private fun hasRegionPermissions(player: Player): Boolean {
        var res = true
        this.worldGuard?.let { worldGuard ->
            {
                val location = player.location
                worldGuard.platform.regionContainer?.let { regionContainer ->
                    regionContainer.get(BukkitAdapter.adapt(location.world))?.let {
                        res = it.getApplicableRegions(BlockVector3.at(location.blockX, location.blockY, location.blockZ))
                            ?.isOwnerOfAll(WorldGuardPlugin.inst().wrapPlayer(player))!! // owner is required to create portals
                    }


                }
            }
        }

        return res
    }
}