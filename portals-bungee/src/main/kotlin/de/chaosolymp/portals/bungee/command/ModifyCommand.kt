package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extension.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender
import java.util.*

class ModifyCommand(private val plugin: BungeePlugin) : SubCommand {

    private fun setName(sender: CommandSender, id: Int, value: String) {
        if (!plugin.portalManager.isNameValid(value)) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.wrong-name"
                )
            )
            return
        }

        if (plugin.portalManager.doesNameExist(value)) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.name-already-exists"
                )
            )
            return
        }

        val name = plugin.portalManager.getNameOfId(id)
        plugin.portalManager.rename(id, value)

        sender.sendMessage(
            plugin.messageConfiguration.getMessage(
                "command.modify.name",
                Replacement("id", id),
                Replacement("origin-name", name),
                Replacement("origin-name", value)
            )
        )
    }

    private fun setDisplayName(sender: CommandSender, id: Int, value: String) {
        plugin.portalManager.setDisplayName(id, value)
        val name = plugin.portalManager.getNameOfId(id)

        sender.sendMessage(
            plugin.messageConfiguration.getMessage(
                "command.modify.display-name",
                Replacement("id", id),
                Replacement("name", name),
                Replacement("display-name", value)
            )
        )
    }

    private fun setPublic(sender: CommandSender, id: Int, public: Boolean) {
        plugin.portalManager.setPublic(id, public)
        val name = plugin.portalManager.getNameOfId(id)

        if (public) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "command.modify.public",
                    Replacement("id", id),
                    Replacement("name", name)
                )
            )
        } else {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "command.modify.private",
                    Replacement("id", id),
                    Replacement("name", name)
                )
            )
        }
    }

    override fun execute(sender: CommandSender, args: Array<out String>?) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.modify")) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        // Validate argument count
        if (args == null || args.size <= 2) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.wrong-syntax",
                    Replacement("syntax", "/portal modify <portal id|name> <name|public|display-name> <value>")
                )
            )
            return
        }

        val portal = args[0]
        val option = args[1]

        // Use user-provided id if user entered a valid numeric value > 0
        // Otherwise find id in database by its name
        val id = if (NumberUtils.isNumber(portal)) {
            portal.toInt()
        } else {
            plugin.portalManager.getIdOfName(portal)
        }

        if (plugin.portalManager.doesPlayerOwnPortalOrHasOtherAccess(sender, id)) {
            when {
                option.equals("name", true) -> {
                    val value = args[2]
                    setName(sender, id, value)
                }
                option.equals("public", true) -> {
                    val value = args[2]
                    setPublic(sender, id, value == "true")
                }
                option.equals("display-name", true) -> {
                    val value = Arrays.copyOfRange(args, 2, args.size).joinToString(" ")
                    setDisplayName(sender, id, value)
                }
            }
        }
    }

}