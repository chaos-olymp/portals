package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extensions.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender

class InfoCommand(private val plugin: BungeePlugin) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.info")) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        // Validate argument count
        if (args?.size != 1) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage(
                "error.wrong-syntax",
                Replacement("syntax", "/portal info <name>")))
            return
        }

        // Find portal by id if `sender` provided a valid numeric number > 0
        // Otherwise find portal by name
        val portal = if (NumberUtils.isUnsignedNumber(args[0])) {
            this.plugin.portalManager.getPortal(args[0].toInt())
        } else {
            this.plugin.portalManager.getPortal(args[0])
        }

        // Portal does not exist
        if (portal == null) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-exists"))
            return
        }

        // Send information message
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
