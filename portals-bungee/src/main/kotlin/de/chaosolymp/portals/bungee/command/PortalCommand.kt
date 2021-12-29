package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.DebugMessenger
import de.chaosolymp.portals.bungee.extension.sendMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
        commandRegistry["cleanup"] = CleanupCommand(plugin)
        commandRegistry["tp"] = TeleportCommand(plugin)
        commandRegistry["help"] = HelpCommand(plugin, this)
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        try {
            DebugMessenger.info("Command Execution", "${sender.name} executed /portal ${args.joinToString(" ")}")

            val cmd = if(args.isNotEmpty()) {
                commandRegistry[args[0]] ?: commandRegistry["help"]
            } else {
                sender.sendMessage(plugin.messageConfiguration.getMessage("error.subcommand-not-exists"))
                commandRegistry["help"]
            }
            runBlocking {
                val startTime = System.currentTimeMillis()
                val job = launch(plugin.coroutineDispatcher) {
                    cmd?.execute(sender, if(args.isEmpty()) args else args.copyOfRange(1, args.size))
                }
                job.invokeOnCompletion { throwable ->
                    val now = System.currentTimeMillis()
                    val diff = now - startTime
                    if(throwable != null) {
                        sender.sendMessage(plugin.messageConfiguration.getMessage("error.exception-occurred"))
                        plugin.exceptionHandler.uncaughtException(Thread.currentThread(), throwable)
                        if(throwable is Exception) {
                            DebugMessenger.exception("Command Execution", throwable)
                        }
                    }
                    if(diff > 3_000) {
                        DebugMessenger.warning("Command Execution", "Command execution took ${diff}ms")
                    } else {
                        DebugMessenger.verbose("Command Execution", "Command execution took ${diff}ms")
                    }
                }
            }
        } catch (ex: Exception) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.exception-occurred"))
            plugin.exceptionHandler.uncaughtException(Thread.currentThread(), ex)
            DebugMessenger.exception("Command Execution", ex)
        }
    }

}