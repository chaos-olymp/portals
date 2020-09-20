package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender

class InfoCommand(private val plugin: BungeePlugin) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        if (sender.hasPermission("portals.info")) {
            if (args?.size == 1) {
                val portal = if (NumberUtils.isNumber(args[0])) {
                    this.plugin.portalManager.getPortal(args[0].toInt())
                } else {
                    this.plugin.portalManager.getPortal(args[0])
                }

                if (portal != null) {
                    sender.sendMessage(
                        this.plugin.messageConfiguration.getMessage(
                            "command.info",
                            Replacement("name", portal.name),
                            Replacement("display-name", portal.displayName ?: portal.name),
                            Replacement("id", portal.id),
                            Replacement(
                                "owner",
                                (this.plugin.proxy.getPlayer(portal.owner) ?: portal.owner.toString()) ?: "Server"
                            ), // If player name cannot be retrieved it prints the uuid
                            Replacement("public", if (portal.public) "✓" else "×"),
                            Replacement("created", portal.created.toString()),
                            Replacement("updated", portal.updated.toString())
                        )
                    )
                } else {
                    sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-exists"))
                }
            }
        } else {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
        }
    }
}