package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.config.Replacement
import de.chaosolymp.portals.bungee.extension.sendData
import de.chaosolymp.portals.bungee.extension.sendMessage
import de.chaosolymp.portals.core.message.proxy_to_server.CleanupRequestPluginMessage
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.CommandSender

class CleanupCommand(private val plugin: BungeePlugin) : SubCommand {
    override suspend fun execute(sender: CommandSender, args: Array<out String>?)  = withContext(plugin.coroutineDispatcher) {
        // Send error message if `sender` has not the required permission
        if (!sender.hasPermission("portals.cleanup")) {
            sender.sendMessage(plugin.messageConfiguration.getMessage("error.no-permission"))
            return@withContext
        }

        val worker = Worker(plugin)
        plugin.proxy.scheduler.runAsync(plugin, worker)
    }

    private class Worker(private val plugin: BungeePlugin): Runnable {
        override fun run() {
            val itemsToFetch = 100

            val portalCount = plugin.portalManager.countPortals()
            getPlayersToNotify().forEach { p -> p.sendMessage(plugin.messageConfiguration.getMessage("command.cleanup.start", Replacement("portal-count", "$portalCount"))) }

            var current = 0

            while(current < portalCount) {
                val portals = plugin.portalManager.getPortals(current, itemsToFetch)

                for (portal in portals) {
                    plugin.proxy.servers[portal.server]?.sendData(
                        CleanupRequestPluginMessage(
                            portal.world,
                            portal.x,
                            portal.y,
                            portal.z
                        )
                    )
                }
                getPlayersToNotify().forEach { p ->
                    p.sendMessage(
                        plugin.messageConfiguration.getMessage(
                            "command.cleanup.progress",
                            Replacement("portal-count", "$portalCount"),
                            Replacement("processed-items", current)
                        )
                    )
                }

                current += itemsToFetch
            }

            getPlayersToNotify().forEach { p -> p.sendMessage(plugin.messageConfiguration.getMessage("command.cleanup.end", Replacement("portal-count", "$portalCount"))) }
        }

        private fun getPlayersToNotify() = plugin.proxy.players.filter { p -> p.hasPermission("portals.cleanup") }

    }

}