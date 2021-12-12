package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extension.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer

class LinkCommand(private val plugin: BungeePlugin) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.link")) {
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

        // Validate argument count
        if (args == null || args.size != 2) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.wrong-syntax",
                    Replacement("syntax", "/portal link <origin> <link>")
                )
            )
            return
        }
        val origin = args[0]
        val target = args[1]

        // Use user-provided id if user entered a valid numeric value > 0
        // Otherwise find id in database by its name
        val originId: Int? = if (NumberUtils.isUnsignedNumber(origin)) {
            Integer.parseUnsignedInt(origin)
        } else {
            if(plugin.portalManager.doesNameExist(origin)) {
                plugin.portalManager.getIdOfName(origin)
            } else {
                null
            }
        }

        // Use user-provided id if user entered a valid numeric value > 0
        // Otherwise find id in database by its name
        val targetId = if (NumberUtils.isUnsignedNumber(target)) {
            Integer.parseUnsignedInt(target)
        } else {
            if(plugin.portalManager.doesNameExist(target)) {
                plugin.portalManager.getIdOfName(target)
            } else {
                null
            }
        }

        // Send error message if `originId` does not exist
        if (originId == null || !this.plugin.portalManager.doesIdExists(originId)) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.origin-not-exists"
                )
            )
            return
        }

        // Send error message if `targetId` does not exist
        if (targetId == null || !this.plugin.portalManager.doesIdExists(targetId)) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.link-not-exists"
                )
            )
            return
        }

        // Send error message if neither player own the origin portal
        // nor the player has admin permission
        if (!plugin.portalManager.doesPlayerOwnPortal(sender.uniqueId, originId)) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.no-access-to-portal"
                )
            )
            return
        }

        // Send error message if the target portal is not public and the player does not own the portal
        if (!(plugin.portalManager.isPublic(targetId) || plugin.portalManager.doesPlayerOwnPortal(
                sender.uniqueId,
                targetId
            ))
        ) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.no-access-to-linked-portal"
                )
            )
            return
        }

        // Do link operation on database
        plugin.portalManager.link(originId, targetId)

        // Send result message
        sender.sendMessage(
            plugin.messageConfiguration.getMessage(
                "command.link",
                Replacement("origin-name", plugin.portalManager.getNameOfId(originId)),
                Replacement("origin-id", originId),
                Replacement("link-name", plugin.portalManager.getNameOfId(targetId)),
                Replacement("link-id", targetId)
            )
        )
    }
}





