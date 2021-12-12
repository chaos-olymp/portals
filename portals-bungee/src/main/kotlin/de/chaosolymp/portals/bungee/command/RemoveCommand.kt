package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extension.sendMessage
import de.chaosolymp.portals.core.LocationResponse
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.concurrent.CompletableFuture

class RemoveCommand(private val plugin: BungeePlugin) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>?) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.remove")) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        // Send error message if `sender` is not an instance of `ProxiedPlayer`
        // We need this, because we require a Location of the player
        // The console is not able to provide a Location
        if (sender !is ProxiedPlayer) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.not-a-player"))
            return
        }

        // Create CompletableFeature and register a callback function using thenAccept
        val locationFuture = CompletableFuture<LocationResponse>()
        locationFuture.thenAccept {
            val portalId = plugin.portalManager.getPortalIdAt(sender.server.info.name, it.world, it.x, it.y, it.z)
            processRemove(portalId, sender)
        }

        // Send request plugin message to the server
        plugin.pluginMessageListener.requestLocation(sender, locationFuture)
    }

    private fun processRemove(portalId: Int?, sender: ProxiedPlayer) {

        // Send message if the portal does not exist
        if (portalId == null) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.not-exists"))
            return
        }

        // Send message if the player has no access to the portal
        if (!plugin.portalManager.doesPlayerOwnPortalOrHasOtherAccess(sender, portalId)) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-access-to-portal"))
            return
        }

        val cachedName = plugin.portalManager.getNameOfId(portalId)
        val portalObj = plugin.portalManager.getPortal(portalId)!!

        // Remove portal from database
        plugin.portalManager.remove(portalId)

        // Send destroy request for the portal block (END_PORTAL_FRAME)
        plugin.pluginMessageListener.sendBlockDestroy(
            portalObj.server,
            portalObj.world,
            portalObj.x,
            portalObj.y,
            portalObj.z
        )

        // Send success message
        sender.sendMessage(
            plugin.messageConfiguration.getMessage(
                "command.remove",
                Replacement("id", portalId),
                Replacement(
                    "name", cachedName
                )
            )
        )
    }
}