package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.sendMessage
import de.chaosolymp.portals.core.NumberUtils
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer

class LinkCommand(private val plugin: BungeePlugin) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        if (sender.hasPermission("portals.link")) {
            if (sender is ProxiedPlayer) {
                if (args != null && args.size == 2) {
                    val origin = args[0]
                    val target = args[1]

                    val originId = if (NumberUtils.isUnsignedNumber(origin)) {
                        Integer.parseUnsignedInt(origin)
                    } else {
                        plugin.portalManager.getIdOfName(origin)
                    }

                    val targetId = if (NumberUtils.isUnsignedNumber(target)) {
                        Integer.parseUnsignedInt(target)
                    } else {
                        plugin.portalManager.getIdOfName(target)
                    }

                    if (this.plugin.portalManager.doesIdExists(originId)) {
                        if (this.plugin.portalManager.doesIdExists(targetId)) {
                            if (plugin.portalManager.doesPlayerOwnPortal(sender.uniqueId, originId)) {
                                if (plugin.portalManager.isPublic(targetId) || plugin.portalManager.doesPlayerOwnPortal(
                                        sender.uniqueId,
                                        targetId
                                    )
                                ) {
                                    plugin.portalManager.link(originId, targetId)
                                    this.plugin.messageConfiguration.getMessage(
                                        "command.link",
                                        Replacement("origin-name", plugin.portalManager.getNameOfId(originId)),
                                        Replacement("origin-id", originId),
                                        Replacement("link-name", plugin.portalManager.getNameOfId(targetId)),
                                        Replacement("link-id", targetId)

                                    )
                                } else {
                                    this.plugin.messageConfiguration.getMessage(
                                        "error.no-access-to-linked-portal"
                                    )
                                }
                            } else {
                                this.plugin.messageConfiguration.getMessage(
                                    "error.no-access-to-portal"
                                )
                            }
                        } else {
                            this.plugin.messageConfiguration.getMessage(
                                "error.link-not-exists"
                            )
                        }
                    } else {
                        this.plugin.messageConfiguration.getMessage(
                            "error.origin-not-exists"
                        )
                    }
                } else {

                    sender.sendMessage(
                        this.plugin.messageConfiguration.getMessage(
                            "error.wrong-syntax",
                            Replacement("syntax", "/portal link <origin> <link>")
                        )
                    )
                }
            } else {
                sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.not-a-player"))
            }
        } else {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
        }

    }
}