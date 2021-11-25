package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender

class InfoCommand(private val plugin: BungeePlugin) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        if (!sender.hasPermission("portals.info")) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }
        if (args?.size != 1) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-exists"))
            return
        }
        val portal = if (NumberUtils.isUnsignedNumber(args[0])) {
            this.plugin.portalManager.getPortal(args[0].toInt())
        } else {
            this.plugin.portalManager.getPortal(args[0])
        }

        if (portal == null) return

        sender.sendMessage(
            this.plugin.messageConfiguration.getMessage(
                "command.info",
                Replacement("name", portal.name),
                Replacement("display-name", portal.displayName ?: portal.name),
                Replacement("id", portal.id),
                Replacement(
                    "owner",
                    (this.plugin.proxy.getPlayer(portal.owner) ?: portal.owner.toString())
                ), // If player name cannot be retrieved it prints the uuid
                Replacement("public", if (portal.public) "✓" else "×"),
                Replacement("created", portal.created.toString()),
                Replacement("updated", portal.updated.toString())
            )
        )

    }
}
