package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.extensions.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender
import java.util.*

class ModifyCommand(private val plugin: BungeePlugin) : SubCommand {

    override fun execute(sender: CommandSender, args: Array<out String>?) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.modify")) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        // Validate argument count
        if (args == null || args.size <= 2) {
            sender.sendMessage(
                this.plugin.messageConfiguration.getMessage(
                    "error.no-access-to-portal"
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
            this.plugin.portalManager.getIdOfName(portal)
        }

        if (this.plugin.portalManager.doesPlayerOwnPortalOrHasOtherAccess(sender, id)) {
            when {
                option.equals("name", true) -> {
                    val value = args[2]
                    if (this.plugin.portalManager.isNameValid(value)) {
                        if (!this.plugin.portalManager.doesNameExist(value)) {
                            this.plugin.portalManager.rename(id, value)
                        } else {
                            sender.sendMessage(
                                this.plugin.messageConfiguration.getMessage(
                                    "error.name-already-exists"
                                )
                            )
                        }
                    } else {
                        sender.sendMessage(
                            this.plugin.messageConfiguration.getMessage(
                                "error.wrong-name"
                            )
                        )
                    }
                }
                option.equals("public", true) -> {
                    val value = args[2]
                    this.plugin.portalManager.setPublic(id, value == "true")
                }
                option.equals("display-name", true) -> {
                    val value = Arrays.copyOfRange(args, 2, args.size).joinToString(" ")
                    this.plugin.portalManager.setDisplayName(id, value)
                }
            }
        }
    }

}