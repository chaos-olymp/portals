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
        if (sender.hasPermission("portals.create")) {
            if (sender is ProxiedPlayer) {
                if (args != null && args.size == 1) {
                    val name = args[0]
                    if (this.plugin.portalManager.isNameValid(name)) {
                        if (!this.plugin.portalManager.doesNameExist(name)) {
                            val locationFuture = CompletableFuture<LocationResponse>()
                            this.plugin.pluginMessageListener.requestLocation(sender, locationFuture)
                            locationFuture.thenAccept {
                                if (it.canCreatePortal) {
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
                                    if (id != null) {
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
                                    } else {
                                        sender.sendMessage(
                                            this.plugin.messageConfiguration.getMessage(
                                                "error.database-error"
                                            )
                                        )
                                    }
                                } else {
                                    sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-region-access"))
                                }
                            }
                        } else {
                            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.database-error"))
                        }
                    } else {
                        sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.wrong-name"))
                    }
                } else {
                    sender.sendMessage(
                        this.plugin.messageConfiguration.getMessage(
                            "error.wrong-syntax",
                            Replacement("syntax", "/portal create <name>")
                        )
                    )
                }
            } else {
                sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-a-player"))
            }
        } else {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
        }
    }
}