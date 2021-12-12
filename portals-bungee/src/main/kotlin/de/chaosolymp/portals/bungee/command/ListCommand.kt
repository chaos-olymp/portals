package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.core.PortalListType
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extension.sendMessage
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.hover.content.Text

class ListCommand(private val plugin: BungeePlugin) : SubCommand {
    private val itemsPerPage = 8

    override fun execute(sender: CommandSender, args: Array<out String>?) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.list")) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        // Validate arguments
        val page = if (args?.size!! > 0) {
            args[0].toUIntOrNull()?.toInt() ?: 1
        } else {
            1
        }

        var mode = PortalListType.OWN

        if (args.contains("-all")) {
            // If the user provided a `-all` flag we set mode to PortalListType.ALL
            // Send an error message if the user has no permission
            if (sender.hasPermission("portals.list.all")) {
                mode = PortalListType.ALL
            } else {
                sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
                return
            }
        }
        else if (args.contains("-public")) {
            // If the user provided a `--public` flag we set mode to PortalListType.PUBLIC
            // Send an error message if the user has no permission
            if (sender.hasPermission("portals.list.public")) {
                mode = PortalListType.PUBLIC
            } else {
                sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
                return
            }
        }

        // Send message if no valid page provided
        if (page == 0) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.pagination.unknown-number"))
            return
        }

        val count = plugin.portalManager.countPortals()
        val maxPages: Int = count / itemsPerPage // we don't want a Double -> Int

        // Send message if page is out of range
        if (maxPages < page) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.pagination.not-exists"))
            return
        }

        val skip = (page - 1) * itemsPerPage
        val result = plugin.portalManager.getPortals(sender, mode, skip, itemsPerPage)

        // Send header component
        sender.sendMessage(
            this.plugin.messageConfiguration.getMessage(
                "command.list.header",
                Replacement("current-page", page),
                Replacement("max-pages", maxPages)
            )
        )

        // Create click-event navigation
        val badgeBuilder = ComponentBuilder()
        if (sender.hasPermission("portals.list.all") && mode != PortalListType.ALL) {
            val badge = plugin.messageConfiguration.getMessage("messages.command.list.badge.all.text")
            badge.forEach {
                it.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/portals list $page -all")
                it.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text(plugin.messageConfiguration.getMessage("messages.command.list.badge.all.hover"))
                )
            }
            badgeBuilder.append(badge)
            badgeBuilder.append(" ")
        } else if (sender.hasPermission("portals.list.public") && mode != PortalListType.ALL) {
            val badge = plugin.messageConfiguration.getMessage("messages.command.list.badge.public.text")
            badge.forEach {
                it.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/portals list $page -public")
                it.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text(plugin.messageConfiguration.getMessage("messages.command.list.badge.public.hover"))
                )
            }
            badgeBuilder.append(badge)
            badgeBuilder.append(" ")
        } else if (mode != PortalListType.OWN) {
            val badge = plugin.messageConfiguration.getMessage("messages.command.list.badge.own.text")
            badge.forEach {
                it.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/portals list $page")
                it.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text(plugin.messageConfiguration.getMessage("messages.command.list.badge.own.hover"))
                )
            }
            badgeBuilder.append(badge)
            badgeBuilder.append(" ")
        }

        result.forEach { portal ->
            // Create list component
            val component = plugin.messageConfiguration.getMessage(
                "command.list.component",
                Replacement("name", portal.name),
                Replacement("display-name", portal.displayName ?: portal.name),
                Replacement("id", portal.id),
                Replacement(
                    "owner",
                    (plugin.proxy.getPlayer(portal.owner) ?: portal.owner.toString())
                ), // If player name cannot be retrieved it prints the uuid
                Replacement("public", if (portal.public) "✓" else "×"),
                Replacement("created", portal.created.toString()),
                Replacement("updated", portal.updated.toString())
            )

            // Set click and hover events for these components
            component.forEach {
                it.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/portals info ${portal.id}")
                it.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text(plugin.messageConfiguration.getMessage("command.list.hover"))
                )
            }

            // Send list component
            sender.sendMessage(component)
        }

        // Provide navigation controls
        val paginationBuilder = ComponentBuilder()
        val hasPrevious = page > 1
        val hasNext = page < maxPages

        if (hasPrevious) {
            paginationBuilder.append("< ").color(ChatColor.AQUA)
            paginationBuilder.event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/portal list ${page - 1}"))
        }

        paginationBuilder.append("-----").color(ChatColor.DARK_AQUA)

        if (hasNext) {
            paginationBuilder.append(" >").color(ChatColor.AQUA)
            paginationBuilder.event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/portal list ${page + 1}"))
        }

        // Send navigation controls
        sender.sendMessage(paginationBuilder.create())

        // Send list footer
        sender.sendMessage(
            plugin.messageConfiguration.getMessage(
                "command.list.footer",
                Replacement("current-page", page),
                Replacement("max-pages", maxPages)
            )
        )

    }
}
