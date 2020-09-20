package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.sendMessage
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.ClickEvent

class HelpCommand(private val plugin: BungeePlugin, private val root: PortalCommand) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        this.root.commandRegistry.keys.forEach { key ->
            val components = this.plugin.messageConfiguration.getMessage("command.help.component", Replacement("sub-command", key))
            components.forEach {
                it.clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/portal $key")
            }
            sender.sendMessage(components)
        }
    }

}
