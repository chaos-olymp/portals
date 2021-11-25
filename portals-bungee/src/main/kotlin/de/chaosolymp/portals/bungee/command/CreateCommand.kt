package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.sendMessage
import de.chaosolymp.portals.core.LocationResponse
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.concurrent.CompletableFuture

class CreateCommand(private val plugin: BungeePlugin) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        if (!sender.hasPermission("portals.create")) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        if (sender !is ProxiedPlayer) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-a-player"))
            return
        }

        if (args == null || args.size != 1) {
            sender.sendMessage(
                this.plugin.messageConfiguration.getMessage(
                    "error.wrong-syntax",
                    Replacement("syntax", "/portal create <name>")
                )
            )
            return
        }

        val name = args[0]
        if (!this.plugin.portalManager.isNameValid(name)) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.wrong-name"))
            return
        }
        if (!this.plugin.portalManager.doesNameExist(name)) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.database-error"))
            return
        }
        val locationFuture = CompletableFuture<LocationResponse>()
        this.plugin.pluginMessageListener.requestLocation(sender, locationFuture)
        locationFuture.thenAccept {
            if (!it.canCreatePortal) {
                sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-region-access"))
                return@thenAccept
            }
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
            if (id == null) {
                sender.sendMessage(
                    this.plugin.messageConfiguration.getMessage(
                        "error.database-error"
                    )
                )
                return@thenAccept
            }
            val blockChangeFuture = CompletableFuture<Void>()
            this.plugin.pluginMessageListener.sendBlockChange(sender, blockChangeFuture)
            blockChangeFuture.thenAccept {
                sender.sendMessage(
                    this.plugin.messageConfiguration.getMessage(
                        "command.create",
                        Replacement("name", name),
                        Replacement("id", id)
                    )
                )
            }
        }
    }
}