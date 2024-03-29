package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extension.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.CommandSender
import java.util.*

class ModifyCommand(private val plugin: BungeePlugin) : SubCommand {

    private suspend fun setName(sender: CommandSender, id: Int, value: String) = withContext(plugin.coroutineDispatcher) {
        if (!plugin.portalManager.isNameValid(value)) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.wrong-name"
                )
            )
            return@withContext
        }

        if (plugin.portalManager.doesNameExist(value)) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.name-already-exists"
                )
            )
            return@withContext
        }

        val name = plugin.suspendingPortalManager.getNameOfId(id)
        plugin.suspendingPortalManager.rename(id, value)

        sender.sendMessage(
            plugin.messageConfiguration.getMessage(
                "command.modify.name",
                Replacement("id", id),
                Replacement("origin-name", name),
                Replacement("origin-name", value)
            )
        )
    }

    private suspend fun setDisplayName(sender: CommandSender, id: Int, value: String) = withContext(plugin.coroutineDispatcher) {
        plugin.suspendingPortalManager.setDisplayName(id, value)
        val name = plugin.suspendingPortalManager.getNameOfId(id)

        sender.sendMessage(
            plugin.messageConfiguration.getMessage(
                "command.modify.display-name",
                Replacement("id", id),
                Replacement("name", name),
                Replacement("display-name", value)
            )
        )
    }

    private suspend fun setPublic(sender: CommandSender, id: Int, public: Boolean) = withContext(plugin.coroutineDispatcher) {
        plugin.suspendingPortalManager.setPublic(id, public)
        val name = plugin.suspendingPortalManager.getNameOfId(id)

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

    override suspend fun execute(sender: CommandSender, args: Array<out String>?) = withContext(plugin.coroutineDispatcher) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.modify")) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
            return@withContext
        }

        // Validate argument count
        if (args == null || args.size <= 2) {
            sender.sendMessage(
                plugin.messageConfiguration.getMessage(
                    "error.wrong-syntax",
                    Replacement("syntax", "/portal modify <portal id|name> <name|public|display-name> <value>")
                )
            )
            return@withContext
        }

        val portal = args[0]
        val option = args[1]

        // Use user-provided id if user entered a valid numeric value > 0
        // Otherwise find id in database by its name
        val id = if (NumberUtils.isNumber(portal)) {
            portal.toInt()
        } else {
            plugin.suspendingPortalManager.getIdOfName(portal)
        }

        if (plugin.suspendingPortalManager.doesPlayerOwnPortalOrHasOtherAccess(sender, id)) {
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