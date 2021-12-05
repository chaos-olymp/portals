package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.DebugMessenger
import de.chaosolymp.portals.bungee.extension.sendMessage
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command

class PortalCommand(private val plugin: BungeePlugin) : Command("portal") {

    internal val commandRegistry = mutableMapOf<String, SubCommand>()

    init {
        commandRegistry["create"] = CreateCommand(plugin)
        commandRegistry["debug"] = DebugCommand(plugin)
        commandRegistry["link"] = LinkCommand(plugin)
        commandRegistry["list"] = ListCommand(plugin)
        commandRegistry["modify"] = ModifyCommand(plugin)
        commandRegistry["info"] = InfoCommand(plugin)
        commandRegistry["remove"] = RemoveCommand(plugin)
        commandRegistry["check"] = CheckCommand(plugin)
        commandRegistry["help"] = HelpCommand(plugin, this)
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        try {
            DebugMessenger.info("Command Execution", "${sender.name} executed /portal ${args.joinToString(" ")}")

            val cmd = if(args.isNotEmpty()) {
                commandRegistry[args[0]] ?: commandRegistry["help"]
            } else {
                sender.sendMessage(plugin.messageConfiguration.getMessage("messages.error.subcommand-not-exists"))
                commandRegistry["help"]
            }

            cmd?.execute(sender, if(args.isEmpty()) args else args.copyOfRange(1, args.size))
        } catch (ex: Exception) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("messages.error.exception-occurred"))
            plugin.exceptionHandler.uncaughtException(Thread.currentThread(), ex)
            DebugMessenger.exception("Command Execution", ex)
        }
    }

}