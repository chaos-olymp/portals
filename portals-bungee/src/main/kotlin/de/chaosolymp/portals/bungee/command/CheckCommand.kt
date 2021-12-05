package de.chaosolymp.portals.bungee.command

import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.extension.sendData
import de.chaosolymp.portals.bungee.extension.sendMessage
import de.chaosolymp.portals.core.message.proxy_to_server.ServerInformationRequestPluginMessage
import de.chaosolymp.portals.core.message.server_to_proxy.ServerInformationResponsePluginMessage
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.config.ServerInfo
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CheckCommand(private val plugin: BungeePlugin) : SubCommand {
    override fun execute(sender: CommandSender, args: Array<out String>?) {
        if(!sender.hasPermission("portals.check")) {
            sender.sendMessage(this.plugin.messageConfiguration.getMessage("error.no-permission"))
            return
        }

        sender.sendMessage(ComponentBuilder("Software Check")
            .color(ChatColor.BLUE)
            .create())

        val proxyComponent = checkProxy().toBaseComponent("Proxy")
        sender.sendMessage(proxyComponent)

        val databaseComponent = checkDatabase().toBaseComponent("Database")
        sender.sendMessage(databaseComponent)

        plugin.proxy.servers.values.forEach { serverInfo ->
            val serverComponent = checkServer(serverInfo).toBaseComponent("Server ${serverInfo.name}")
            sender.sendMessage(serverComponent)
        }

        sender.sendMessage(ComponentBuilder("Software Check done")
            .color(ChatColor.BLUE)
            .create())
    }

    private fun checkProxy(): CheckResult {
        if(plugin.exceptionHandler.exceptionCount > 0) {
            return CheckResult(CheckResultType.Fatal, "${plugin.exceptionHandler.exceptionCount} Exceptions occurred")
        }

        return CheckResult(CheckResultType.Success, "No incidents reported")
    }

    private fun checkDatabase(): CheckResult {
        if(plugin.databaseConfiguration.dataSource.isRunning && !plugin.databaseConfiguration.dataSource.isClosed) {
            val beforeDbOperation = Instant.now()
            val dbTime = plugin.portalManager.getDbTime()
            val afterDbOperation = Instant.now()

            if(afterDbOperation < dbTime) {
                return CheckResult(CheckResultType.Warning, "Database time is out of sync - Database: $dbTime Server: $afterDbOperation")
            }

            val latency = Duration.between(beforeDbOperation, dbTime)
            val latencyMillis = abs(latency.toMillis())

            if(latencyMillis > 3_000) {
                return CheckResult(CheckResultType.Warning, "High database latency - Latency: ${latencyMillis}ms")
            }

            return CheckResult(CheckResultType.Success, "Everything is okay - Latency: ${latencyMillis}ms")
        }
        return CheckResult(CheckResultType.Fatal, "Connection is closed")
    }

    private fun checkServer(serverInfo: ServerInfo): CheckResult {
        val pluginVersion = plugin.description.version
        val future = CompletableFuture<ServerInformationResponsePluginMessage>()
        plugin.pluginMessageListener.serverInformationResponse = future

        val startTimestamp = Instant.now()
        val message = ServerInformationRequestPluginMessage(
            pluginVersion,
            startTimestamp.toEpochMilli()
        )
        serverInfo.sendData(message)

        try {
            val result = future.get(5, TimeUnit.SECONDS)
            if(result.pluginVersion != pluginVersion) {
                return CheckResult(CheckResultType.Warning, "Plugin versions different - Server: ${result.pluginVersion} - Proxy: $pluginVersion")
            }

            val latencyMillis = result.timestamp - startTimestamp.toEpochMilli()
            if(latencyMillis > 2_000) {
                return CheckResult(CheckResultType.Warning, "High latency: $latencyMillis")
            }

            return CheckResult(CheckResultType.Success, "Seems good - Latency: $latencyMillis")
        } catch(ex: Exception) {
            return CheckResult(CheckResultType.Fatal, "Response timeout of 5 seconds exceeded")
        }
    }

    private data class CheckResult(val type: CheckResultType, val description: String? = null) {
        fun toBaseComponent(label: String): Array<BaseComponent> {
            val builder = ComponentBuilder()

            val color = when(type) {
                CheckResultType.Success -> ChatColor.GREEN
                CheckResultType.Warning -> ChatColor.GOLD
                CheckResultType.Fatal -> ChatColor.RED
            }

            builder.append("$label: ")
            builder.color(color)
            builder.bold(true)

            builder.append(description ?: "")
            builder.color(color)
            builder.bold(false)

            return builder.create()
        }
    }

    private enum class CheckResultType {
        Success,
        Warning,
        Fatal
    }
}