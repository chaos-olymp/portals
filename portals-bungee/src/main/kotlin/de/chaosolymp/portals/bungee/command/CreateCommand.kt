package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extensions.sendMessage
import de.chaosolymp.portals.core.LocationResponse
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.concurrent.CompletableFuture

class CreateCommand(private val plugin: BungeePlugin) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.create")) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        // Send error message if `sender` is not an instance of `ProxiedPlayer`
        // We need this, because we require a Location of the player
        // The console is not able to provide a Location
        if (sender !is ProxiedPlayer) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-a-player"))
            return
        }

        // Validate argument count
        if (args == null || args.size != 1) {
            sender.sendMessage(
                this.plugin.messageConfiguration.getMessage(
                    "error.wrong-syntax",
                    Replacement("syntax", "/portal create <name>")
                )
            )
            return
        }

        // Validate name conventions
        val name = args[0]
        if (!this.plugin.portalManager.isNameValid(name)) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.wrong-name"))
            return
        }

        // Send error message if this portal name is already present in database
        if (!this.plugin.portalManager.doesNameExist(name)) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.database-error"))
            return
        }

        // Create CompletableFeature and register a callback function using thenAccept
        val locationFuture = CompletableFuture<LocationResponse>()
        locationFuture.thenAccept {

            // Send error message if player has no access to the region
            if (!it.canCreatePortal) {
                sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-region-access"))
                return@thenAccept
            }

            // Create portal in database and use the generated id by AUTO_INCREMENT
            val id = this.plugin.portalManager.createPortal(
                sender.uniqueId,
                name,
                sender.server.info.name,
                false,
                it.world,
                it.x,
                it.y - 1,
                it.z
            )

            // Send error if database returned a uncommon result (No id present)
            if (id == null) {
                sender.sendMessage(
                    this.plugin.messageConfiguration.getMessage(
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
                    this.plugin.messageConfiguration.getMessage(
                        "command.create",
                        Replacement("name", name),
                        Replacement("id", id)
                    )
                )
            }

            // Send block change request to server
            this.plugin.pluginMessageListener.sendBlockChange(sender, blockChangeFuture)
        }

        // Send request plugin message to the server
        this.plugin.pluginMessageListener.requestLocation(sender, locationFuture)
    }
}