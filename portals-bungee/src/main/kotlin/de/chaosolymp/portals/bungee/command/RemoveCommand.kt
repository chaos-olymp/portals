package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender

class RemoveCommand(private val plugin: BungeePlugin) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>?) {
        if (sender.hasPermission("portals.remove")) {
            if (args != null && args.size == 1) {
                val portal = args[0]

                if (plugin.portalManager.doesNameOrIdExist(portal)) {
                    val id = if (NumberUtils.isUnsignedNumber(portal)) {
                        Integer.parseUnsignedInt(portal)
                    } else {
                        plugin.portalManager.getIdOfName(portal)
                    }

                    if (plugin.portalManager.doesPlayerOwnPortalOrHasOtherAccess(sender, id)) {
                        val cachedName = plugin.portalManager.getNameOfId(id)
                        plugin.portalManager.remove(id)
                        sender.sendMessage(
                            this.plugin.messageConfiguration.getMessage(
                                "command.remove",
                                Replacement("id", id),
                                Replacement(
                                    "name", cachedName
                                )
                            )
                        )
                    } else {
                        sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-access-to-portal"))
                    }
                } else {
                    sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-exists"))
                }
            }
        } else {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
        }
    }

}