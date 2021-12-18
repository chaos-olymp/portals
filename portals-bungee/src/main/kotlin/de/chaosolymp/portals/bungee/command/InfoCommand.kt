package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extension.sendMessage
import de.chaosolymp.portals.core.LocationResponse
import de.chaosolymp.portals.core.NumberUtils
import de.chaosolymp.portals.core.Portal
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.concurrent.CompletableFuture

class InfoCommand(private val plugin: BungeePlugin) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.info")) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        if(sender !is ProxiedPlayer) {
            return
        }

        // Validate argument count
        if (args?.size!! > 1) {
            sender.sendMessage(plugin.messageConfiguration.getMessage(
                "error.wrong-syntax",
                Replacement("syntax", "/portal info [name]")))
            return
        }

        if(args.isEmpty()) {
            // Create CompletableFeature and register a callback function using thenAccept
            val locationFuture = CompletableFuture<LocationResponse>()
            locationFuture.thenAccept {
                val portalId = plugin.portalManager.getPortalIdAt(sender.server.info.name, it.world, it.x, it.y, it.z)
                if(portalId == null) {
                    sender.sendMessage(plugin.messageConfiguration.getMessage(
                        "error.wrong-syntax",
                        Replacement("syntax", "/portal info [name]")))
                } else {
                    val portal = plugin.portalManager.getPortal(portalId)
                    processInfo(sender, portal)
                }
            }

            // Send request plugin message to the server
            plugin.pluginMessageListener.requestLocation(sender, locationFuture)
            return
        }

        // Find portal by id if `sender` provided a valid numeric number > 0
        // Otherwise find portal by name
        val portal = if (NumberUtils.isUnsignedNumber(args[0])) {
            plugin.portalManager.getPortal(args[0].toInt())
        } else {
            plugin.portalManager.getPortal(args[0])
        }
        processInfo(sender, portal)
    }

    private fun processInfo(sender: ProxiedPlayer, portal: Portal?) {
        // Portal does not exist
        if (portal == null) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.not-exists"))
            return
        }

        // If player name cannot be retrieved it prints the uuid
        val ownerDisplay = plugin.proxy.getPlayer(portal.owner) ?: portal.owner.toString()

        // Send information message
        sender.sendMessage(
            plugin.messageConfiguration.getMessage(
                "command.info",
                Replacement("name", portal.name),
                Replacement("display-name", portal.displayName ?: portal.name),
                Replacement("id", portal.id),
                Replacement("owner", ownerDisplay),
                Replacement("public", if (portal.public) "✓" else "×"),
                Replacement("created", portal.created.toString()),
                Replacement("updated", portal.updated.toString())
            )
        )
    }
}
