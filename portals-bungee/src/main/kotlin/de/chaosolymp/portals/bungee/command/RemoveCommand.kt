package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender

class RemoveCommand(private val plugin: BungeePlugin) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>?) {
        if (!sender.hasPermission("portals.remove")) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }
        if (args == null || args.size != 1) return

        val portal = args[0]

        if (!plugin.portalManager.doesNameOrIdExist(portal)) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-exists"))
            return
        }
        val id = if (NumberUtils.isUnsignedNumber(portal)) {
            Integer.parseUnsignedInt(portal)
        } else {
            plugin.portalManager.getIdOfName(portal)
        }

        if (!plugin.portalManager.doesPlayerOwnPortalOrHasOtherAccess(sender, id)) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-access-to-portal"))
            return
        }
        val cachedName = plugin.portalManager.getNameOfId(id)
        val portalObj = plugin.portalManager.getPortal(id)!!
        plugin.portalManager.remove(id)
        plugin.pluginMessageListener.sendBlockDestroy(
            portalObj.server,
            portalObj.world,
            portalObj.x,
            portalObj.y,
            portalObj.z
        )
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