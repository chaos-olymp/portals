package de.chaosolymp.portals.bungee.listener

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.core.message.generated.deserialize
import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.DebugMessenger
import de.chaosolymp.portals.bungee.extension.sendData
import de.chaosolymp.portals.core.*
import de.chaosolymp.portals.core.message.proxy_to_server.*
import de.chaosolymp.portals.core.message.server_to_proxy.*
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.Connection
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.*
import java.util.concurrent.CompletableFuture

class PluginMessageListener(val plugin: BungeePlugin) : Listener {

    private val map = mutableMapOf<UUID, CompletableFuture<LocationResponse>>()
    internal var serverInformationResponse: CompletableFuture<ServerInformationResponsePluginMessage>? = null

    private val blockChangeMap = mutableMapOf<UUID, CompletableFuture<Void>>()

    private fun handleLocationResponsePluginMessage(deserialized: LocationResponsePluginMessage, connection: Connection) {
        if (map.containsKey(deserialized.uuid)) {
            map[deserialized.uuid]?.complete(LocationResponse(deserialized.canCreatePortal, deserialized.world, deserialized.x, deserialized.y, deserialized.z))
            map.remove(deserialized.uuid)
        } else {
            plugin.proxy.logger.warning("${connection.socketAddress} sent location request for non-requested uuid ${deserialized.uuid}.")
        }
    }

    private fun handleAuthorizeTeleportRequestPluginMessage(deserialized: AuthorizeTeleportRequestPluginMessage) {
        val player = plugin.proxy.getPlayer(deserialized.uuid)
        val server = player.server.info.name

        val id = plugin.portalManager.getPortalIdAt(server, deserialized.world, deserialized.x, deserialized.y, deserialized.z)
        if (id == null) {
            plugin.logger.warning("Caught invalid teleportation - World: ${deserialized.world} X: ${deserialized.x} Y: ${deserialized.y} Z: ${deserialized.z}")
            return
        }

        val link = plugin.portalManager.getPortalLink(id)
        if(link == null) {
            plugin.logger.warning("Cannot perform teleportation because portal #${id} is not linked")
            return
        }

        val portal = plugin.portalManager.getPortal(link)
        if(portal == null) {
            plugin.logger.warning("Not expected behavior: Portal #${id} is linked to #${link} but #${link} is not present in database")
            return
        }

        val serverInfo = plugin.proxy.getServerInfo(portal.server)
        if (portal.server != server) {
            player.connect(serverInfo)
            plugin.logger.info("Sent player ${player.name} (${player.uniqueId}) to ${serverInfo.name}")
        } else {
            plugin.logger.info("Cannot connect, the player is on the same server")
        }

        serverInfo.sendData(AuthorizeTeleportResponsePluginMessage(deserialized.uuid, portal.world, portal.x, portal.y, portal.z))
    }

    private fun handleBlockChangeAcceptancePluginMessage(deserialized: BlockChangeAcceptancePluginMessage, connection: Connection) {
        if (blockChangeMap.containsKey(deserialized.uuid)) {
            blockChangeMap[deserialized.uuid]?.complete(null)
            blockChangeMap.remove(deserialized.uuid)
        } else {
            plugin.proxy.logger.warning("${connection.socketAddress} sent block change response for non-requested uuid ${deserialized.uuid}.")
        }
    }

    private fun handleValidationPluginMessage(deserialized: ValidationPluginMessage) {
        val serverInfo = plugin.proxy.getPlayer(deserialized.uuid).server.info

        val valid = plugin.portalManager.getPortalIdAt(serverInfo.name, deserialized.worldName, deserialized.x, deserialized.y, deserialized.z) != null
        serverInfo.sendData(ValidationResponsePluginMessage(deserialized.uuid, deserialized.worldName, deserialized.x, deserialized.y, deserialized.z, valid))
    }

    private fun handleServerInformationResponsePluginMessage(deserialized: ServerInformationResponsePluginMessage) {
        if(serverInformationResponse == null) {
            plugin.proxy.logger.warning("Future for ServerInformationResponsePluginMessage is not present")
        } else {
            serverInformationResponse!!.complete(deserialized)

            // Reset future
            serverInformationResponse = null
        }
    }

    private fun handleRemovePortalPluginMessage(deserialized: RemovePortalPluginMessage) {
        val player = plugin.proxy.getPlayer(deserialized.player)
        val server = player.server.info.name

        val portalId = plugin.portalManager.getPortalIdAt(server, deserialized.world, deserialized.x, deserialized.y, deserialized.z)
        if(portalId != null) {
            plugin.portalManager.remove(portalId)
        }
    }

    @EventHandler
    @Suppress("UnstableApiUsage")
    fun handlePluginMessage(event: PluginMessageEvent) {
        // Only handle BungeeCord requests
        if (!(event.tag == "BungeeCord" || event.tag == "bungeecord:main")) return

        // Deserialize input to message (see AbstractPluginMessage)
        val input = ByteStreams.newDataInput(event.data)
        val deserialized = deserialize(input)

        // Log incoming message for debugging purposes
        if (deserialized != null) {
            plugin.logger.info("Incoming plugin message of type ${deserialized.javaClass} - $deserialized")
        }

        when (deserialized) {
            is LocationResponsePluginMessage -> handleLocationResponsePluginMessage(deserialized, event.sender)
            is AuthorizeTeleportRequestPluginMessage -> handleAuthorizeTeleportRequestPluginMessage(deserialized)
            is BlockChangeAcceptancePluginMessage -> handleBlockChangeAcceptancePluginMessage(deserialized, event.sender)
            is ValidationPluginMessage -> handleValidationPluginMessage(deserialized)
            is ServerInformationResponsePluginMessage -> handleServerInformationResponsePluginMessage(deserialized)
            is RemovePortalPluginMessage -> handleRemovePortalPluginMessage(deserialized)
            else -> plugin.logger.warning("Unknown incoming plugin message")
        }
    }

    fun requestLocation(player: ProxiedPlayer, future: CompletableFuture<LocationResponse>) {
        map[player.uniqueId] = future
        player.server.sendData(RequestLocationPluginMessage(player.uniqueId))
    }

    fun sendBlockChange(player: ProxiedPlayer, future: CompletableFuture<Void>) {
        blockChangeMap[player.uniqueId] = future
        player.server.sendData(BlockChangeRequestPluginMessage(player.uniqueId))
    }

    fun sendBlockDestroy(server: String, world: String, x: Int, y: Int, z: Int) {
        val serverInfo = plugin.proxy.servers[server]
        if(serverInfo == null) {
            plugin.logger.warning("Server $server does not exist")
            DebugMessenger.warning("Messaging", "Cannot send block destroy to server $server (Server not available)")
            return
        }
        serverInfo.sendData(BlockDestroyRequestPluginMessage(world, x, y, z))
    }
}
