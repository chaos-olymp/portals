package de.chaosolymp.portals.bungee.listener

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.core.messages.generated.deserialize
import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.bungee.extensions.sendData
import de.chaosolymp.portals.core.*
import de.chaosolymp.portals.core.messages.proxy_to_server.*
import de.chaosolymp.portals.core.messages.server_to_proxy.*
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
            is LocationResponsePluginMessage -> {
                if (map.containsKey(deserialized.uuid)) {
                    map[deserialized.uuid]?.complete(LocationResponse(deserialized.canCreatePortal, deserialized.world, deserialized.x, deserialized.y, deserialized.z))
                    map.remove(deserialized.uuid)
                } else {
                    this.plugin.proxy.logger.warning("${event.sender.socketAddress} sent location request for non-requested uuid ${deserialized.uuid}.")
                }
            }
            is AuthorizeTeleportRequestPluginMessage -> {
                val server = this.plugin.proxy.getPlayer(deserialized.uuid).server.info.name

                val id = this.plugin.portalManager.getPortalIdAt(server, deserialized.world, deserialized.x, deserialized.y, deserialized.z)
                if (id != null) {
                    val link = this.plugin.portalManager.getPortalLink(id)
                    plugin.logger.info("link = $link")
                    val portal = this.plugin.portalManager.getPortal(link!!)
                    plugin.logger.info("portal = $portal")

                    val serverInfo = this.plugin.proxy.getServerInfo(portal?.server)
                    plugin.logger.info("serverInfo = $serverInfo")
                    if (portal?.server != server) {
                        plugin.logger.info("connecting")
                        plugin.proxy.getPlayer(deserialized.uuid).connect(serverInfo)
                    } else {
                        plugin.logger.info("Cannot connect, the player is on the same server")
                    }

                    serverInfo.sendData(AuthorizeTeleportResponsePluginMessage(deserialized.uuid, portal!!.world, portal.x, portal.y, portal.z))
                    plugin.logger.info("Sent plugin message")
                    if (portal.server != server) {
                        plugin.proxy.getPlayer(deserialized.uuid).connect(serverInfo)
                    }
                } else {
                    plugin.logger.warning("Caught invalid teleportation - World: ${deserialized.world} X: ${deserialized.x} Y: ${deserialized.y} Z: ${deserialized.z}")
                }
            }
            is BlockChangeAcceptancePluginMessage -> {
                if (blockChangeMap.containsKey(deserialized.uuid)) {
                    blockChangeMap[deserialized.uuid]?.complete(null)
                    blockChangeMap.remove(deserialized.uuid)
                } else {
                    plugin.proxy.logger.warning("${event.sender.socketAddress} sent block change response for non-requested uuid ${deserialized.uuid}.")
                }
            }
            is ValidationPluginMessage -> {
                val serverInfo = plugin.proxy.getPlayer(deserialized.uuid).server.info

                val valid = plugin.portalManager.getPortalIdAt(serverInfo.name, deserialized.worldName, deserialized.x, deserialized.y, deserialized.z) != null
                serverInfo.sendData(ValidationResponsePluginMessage(deserialized.uuid, deserialized.worldName, deserialized.x, deserialized.y, deserialized.z, valid))
            }
            is ServerInformationResponsePluginMessage -> {
                if(serverInformationResponse == null) {
                    plugin.proxy.logger.warning("Future for ServerInformationResponsePluginMessage is not present")
                } else {
                    serverInformationResponse!!.complete(deserialized)
                }
            }
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
        plugin.proxy.servers[server]!!.sendData(BlockDestroyRequestPluginMessage(world, x, y, z))
    }
}
