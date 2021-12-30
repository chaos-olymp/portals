package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extension.sendData
import de.chaosolymp.portals.bungee.extension.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import de.chaosolymp.portals.core.message.proxy_to_server.AuthorizeTeleportResponsePluginMessage
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer

class TeleportCommand(private val plugin: BungeePlugin) : SubCommand {
    override suspend fun execute(sender: CommandSender, args: Array<out String>?) = withContext(plugin.coroutineDispatcher) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.teleport")) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
            return@withContext
        }

        // Send error message if `sender` is not an instance of `ProxiedPlayer`
        // We need this, to teleport the player
        if (sender !is ProxiedPlayer) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.not-a-player"))
            return@withContext
        }

        // Validate argument count
        if (args == null || args.size != 1) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.wrong-syntax",
                    Replacement("syntax", "/portal tp <name|id>")
                )
            )
            return@withContext
        }

        // Find portal by id if `sender` provided a valid numeric number > 0
        // Otherwise find portal by name
        val portal = if (NumberUtils.isUnsignedNumber(args[0])) {
            plugin.portalManager.getPortal(args[0].toInt())
        } else {
            plugin.portalManager.getPortal(args[0])
        }

        if(portal == null) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage("error.not-exists")
            )
            return@withContext
        }

        val serverInfo = plugin.proxy.servers[portal.server]!!
        serverInfo.sendData(AuthorizeTeleportResponsePluginMessage(sender.uniqueId, portal.world, portal.x, portal.y, portal.z, portal.yaw, portal.pitch))

        if(sender.server.info != serverInfo) {
            sender.connect(serverInfo)
        }

        sender.sendMessage(
            plugin.messageConfiguration.getMessage("command.teleport.success", Replacement("display-name", portal.displayName ?: portal.name))
        )
    }
}