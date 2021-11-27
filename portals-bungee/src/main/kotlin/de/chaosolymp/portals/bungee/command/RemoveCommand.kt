package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extensions.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender

class RemoveCommand(private val plugin: BungeePlugin) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>?) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.remove")) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        // Validate argument count
        if (args == null || args.size != 1) return

        val portal = args[0]

        // Send message if the portal does not exist
        if (!plugin.portalManager.doesNameOrIdExist(portal)) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-exists"))
            return
        }

        // Use user-provided id if user entered a valid numeric value > 0
        // Otherwise find id in database by its name
        val id = if (NumberUtils.isUnsignedNumber(portal)) {
            Integer.parseUnsignedInt(portal)
        } else {
            plugin.portalManager.getIdOfName(portal)
        }

        // Send message if the player has no access to the portal
        if (!plugin.portalManager.doesPlayerOwnPortalOrHasOtherAccess(sender, id)) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-access-to-portal"))
            return
        }

        val cachedName = plugin.portalManager.getNameOfId(id)
        val portalObj = plugin.portalManager.getPortal(id)!!

        // Remove portal from database
        plugin.portalManager.remove(id)

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
            this.plugin.messageConfiguration.getMessage(
                "command.remove",
                Replacement("id", id),
                Replacement(
                    "name", cachedName
                )
            )
        )

    }
}