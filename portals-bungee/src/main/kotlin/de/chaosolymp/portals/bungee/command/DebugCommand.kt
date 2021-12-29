package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.DebugMessenger
import de.chaosolymp.portals.bungee.extension.sendMessage
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer

class DebugCommand(private val plugin: BungeePlugin) : SubCommand {
    override suspend fun execute(sender: CommandSender, args: Array<out String>?) = withContext(plugin.coroutineDispatcher) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.debug")) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
            return@withContext
        }

        // Send error message if `sender` is not an instance of `ProxiedPlayer`
        if (sender !is ProxiedPlayer) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.not-a-player"))
            return@withContext
        }

        if(DebugMessenger.targetPlayers.contains(sender)) {
            DebugMessenger.targetPlayers.remove(sender)
            sender.sendMessage(plugin.messageConfiguration.getMessage("command.debug.disable"))
            DebugMessenger.info("Debug Mode", "${sender.name} left Debug Mode")
            return@withContext
        }

        DebugMessenger.info("Debug Mode", "${sender.name} joined Debug Mode")
        DebugMessenger.targetPlayers.add(sender)
        sender.sendMessage(plugin.messageConfiguration.getMessage("command.debug.enable"))
    }
}