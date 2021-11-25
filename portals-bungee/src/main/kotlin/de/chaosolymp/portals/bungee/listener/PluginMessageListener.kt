package de.chaosolymp.portals.bungee.listener

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.core.messages.generated.deserialize
import de.chaosolymp.portals.core.messages.generated.serialize
import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.core.*
import de.chaosolymp.portals.core.messages.proxy_to_server.*
import de.chaosolymp.portals.core.messages.server_to_proxy.AuthorizeTeleportRequestPluginMessage
import de.chaosolymp.portals.core.messages.server_to_proxy.BlockChangeAcceptancePluginMessage
import de.chaosolymp.portals.core.messages.server_to_proxy.LocationResponsePluginMessage
import de.chaosolymp.portals.core.messages.server_to_proxy.ValidationPluginMessage
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.*
import java.util.concurrent.CompletableFuture

class PluginMessageListener(val plugin: BungeePlugin) : Listener {

    private val map = mutableMapOf<UUID, CompletableFuture<LocationResponse>>()

    private val blockChangeMap = mutableMapOf<UUID, CompletableFuture<Void>>()

    @EventHandler
    @Suppress("UnstableApiUsage")
    fun handlePluginMessage(event: PluginMessageEvent) {
        if (!(event.tag == "BungeeCord" || event.tag == "bungeecord:main")) return
        val input = ByteStreams.newDataInput(event.data)
        when (val deserialized = deserialize(input)) {
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
                    val portal = this.plugin.portalManager.getPortal(link)
                    plugin.logger.info("portal = $portal")

                    val serverInfo = this.plugin.proxy.getServerInfo(portal?.server)
                    plugin.logger.info("serverInfo = $serverInfo")
                    if (portal?.server != server) {
                        plugin.logger.info("connecting")
                        this.plugin.proxy.getPlayer(deserialized.uuid).connect(serverInfo)
                    } else {
                        plugin.logger.info("Cannot connect, the player is on the same server")
                    }

                    val out = ByteStreams.newDataOutput()

                    val message = AuthorizeTeleportResponsePluginMessage(deserialized.uuid, portal!!.world, portal.x, portal.y, portal.z)
                    serialize(message, out)

                    serverInfo.sendData("BungeeCord", out.toByteArray())
                    plugin.logger.info("Sent plugin message")
                    if (portal.server != server) {
                        this.plugin.proxy.getPlayer(deserialized.uuid).connect(serverInfo)
                    }
                } else {
                    this.plugin.logger.warning("${event.sender.socketAddress}: caught invalid teleportation")
                }
            }
            is BlockChangeAcceptancePluginMessage -> {
                if (blockChangeMap.containsKey(deserialized.uuid)) {
                    blockChangeMap[deserialized.uuid]?.complete(null)
                    blockChangeMap.remove(deserialized.uuid)
                } else {
                    this.plugin.proxy.logger.warning("${event.sender.socketAddress} sent block change response for non-requested uuid ${deserialized.uuid}.")
                }
            }
            is ValidationPluginMessage -> {
                val serverInfo = this.plugin.proxy.getPlayer(deserialized.uuid).server.info

                val valid = plugin.portalManager.getPortalIdAt(serverInfo.name, deserialized.worldName, deserialized.x, deserialized.y, deserialized.z) != null
                val message = ValidationResponsePluginMessage(deserialized.uuid, deserialized.worldName, deserialized.x, deserialized.y, deserialized.z, valid)

                val out = ByteStreams.newDataOutput()
                serialize(message, out)
                serverInfo.sendData("BungeeCord", out.toByteArray())
            }
            else -> {}
        }
    }

    @Suppress("UnstableApiUsage")
    fun requestLocation(player: ProxiedPlayer, future: CompletableFuture<LocationResponse>) {
        map[player.uniqueId] = future

        val output = ByteStreams.newDataOutput()
        serialize(RequestLocationPluginMessage(player.uniqueId), output)

        player.server.sendData("BungeeCord", output.toByteArray())
    }

    @Suppress("UnstableApiUsage")
    fun sendBlockChange(player: ProxiedPlayer, future: CompletableFuture<Void>) {
        blockChangeMap[player.uniqueId] = future

        val output = ByteStreams.newDataOutput()
        serialize(BlockChangeRequestPluginMessage(player.uniqueId), output)

        player.server.sendData("BungeeCord", output.toByteArray())
    }

    @Suppress("UnstableApiUsage")
    fun sendBlockDestroy(server: String, world: String, x: Int, y: Int, z: Int) {

        val output = ByteStreams.newDataOutput()
        serialize(BlockDestroyRequestPluginMessage(world, x, y, z), output)

        plugin.proxy.servers[server]!!.sendData("BungeeCord", output.toByteArray())
    }
}
