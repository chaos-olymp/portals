package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extension.sendMessage
import de.chaosolymp.portals.core.LocationResponse
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.concurrent.CompletableFuture

class CreateCommand(private val plugin: BungeePlugin) : SubCommand {
    override suspend fun execute(sender: CommandSender, args: Array<out String>?) = withContext(plugin.coroutineDispatcher) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.create")) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
            return@withContext
        }

        // Send error message if `sender` is not an instance of `ProxiedPlayer`
        // We need this, because we require a Location of the player
        // The console is not able to provide a Location
        if (sender !is ProxiedPlayer) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.not-a-player"))
            return@withContext
        }

        // Validate argument count
        if (args == null || args.size != 1) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.wrong-syntax",
                    Replacement("syntax", "/portal create <name>")
                )
            )
            return@withContext
        }

        // Validate name conventions
        val name = args[0]
        if (!plugin.portalManager.isNameValid(name)) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.wrong-name"))
            return@withContext
        }

        // Send error message if this portal name is already present in database
        if (plugin.portalManager.doesNameExist(name)) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.name-already-exists"))
            return@withContext
        }

        // Create CompletableFeature and register a callback function using thenAccept
        val locationFuture = CompletableFuture<LocationResponse>()
        locationFuture.thenAccept {

            // Send error message if player has no access to the region
            if (!it.canCreatePortal) {
                sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-region-access"))
                return@thenAccept
            }

            // Create portal in database and use the generated id by AUTO_INCREMENT
            val id = runBlocking {
                return@runBlocking plugin.suspendingPortalManager.createPortal(
                    sender.uniqueId,
                    name,
                    sender.server.info.name,
                    false,
                    it.world,
                    it.x,
                    it.y,
                    it.z,
                    it.yaw,
                    it.pitch
                )
            }

            // Send error if database returned a uncommon result (No id present)
            if (id == null) {
                sender.sendMessage(
                    plugin.messageConfiguration.getMessage(
                        "error.database-error"
                    )
                )
                return@thenAccept
            }

            // Create CompletableFuture
            val blockChangeFuture = CompletableFuture<Void>()

            // Register callback on the future
            blockChangeFuture.thenAccept {
                sender.sendMessage(
                    plugin.messageConfiguration.getMessage(
                        "command.create",
                        Replacement("name", name),
                        Replacement("id", id)
                    )
                )
            }

            // Send block change request to server
            plugin.pluginMessageListener.sendBlockChange(sender, blockChangeFuture)
        }

        // Send request plugin message to the server
        plugin.pluginMessageListener.requestLocation(sender, locationFuture)
    }
}