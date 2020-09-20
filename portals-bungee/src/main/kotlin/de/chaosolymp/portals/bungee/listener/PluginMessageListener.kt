package de.chaosolymp.portals.bungee.listener

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.core.LocationResponse
import de.chaosolymp.portals.core.UUIDUtils
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.*
import java.util.concurrent.CompletableFuture

class PluginMessageListener(val plugin: BungeePlugin) : Listener {

    private val map = mutableMapOf<UUID, CompletableFuture<LocationResponse>>()

    @EventHandler
    fun handlePluginMessage(event: PluginMessageEvent) {
        if(event.tag == "BungeeCord" || event.tag == "bungeecord:main") {
            val input = ByteStreams.newDataInput(event.data)
            val subChannel = input.readUTF()
            if(subChannel == "portals:location") {
                val uuidArray = ByteArray(16)
                input.readFully(uuidArray)
                val uuid = UUIDUtils.getUUIDFromBytes(uuidArray)
                val canCreatePortal = input.readBoolean()
                val world = input.readUTF()
                val x = input.readInt()
                val y = input.readInt()
                val z = input.readInt()

                if (map.containsKey(uuid)) {
                    map[uuid]?.complete(LocationResponse(canCreatePortal, world, x, y, z))
                    map.remove(uuid)
                } else {
                    this.plugin.proxy.logger.warning("${event.sender.socketAddress} sent location request for non-requested uuid $uuid.")
                }
            } else if(subChannel == "portals:authorize_teleport") {
                val uuidArray = ByteArray(16)
                input.readFully(uuidArray)
                val uuid = UUIDUtils.getUUIDFromBytes(uuidArray)
                val world = input.readUTF()
                val x = input.readInt()
                val y = input.readInt()
                val z = input.readInt()

                val server = this.plugin.proxy.getPlayer(uuid).server.info.name

                val id = this.plugin.portalManager.getPortalIdAt(server, world, x, y, z)
                if(id != null) {
                    val link = this.plugin.portalManager.getPortalLink(id)
                    val portal = this.plugin.portalManager.getPortal(link)

                    val serverInfo = this.plugin.proxy.getServerInfo(portal?.server)

                    if(portal?.server != server) {
                        this.plugin.proxy.getPlayer(uuid).connect(serverInfo)
                    }

                    val out = ByteStreams.newDataOutput(42 + portal!!.world.length)
                    out.writeUTF("portals:teleport")
                    out.write(uuidArray)
                    out.writeUTF(portal.world)
                    out.writeInt(portal.x)
                    out.writeInt(portal.y)
                    out.writeInt(portal.z)
                    serverInfo.sendData("BungeeCord", out.toByteArray())


                } else {
                    this.plugin.logger.warning("${event.sender.socketAddress}: caught invalid teleportation")
                }
            }
        }
    }

    fun requestLocation(player: ProxiedPlayer, future: CompletableFuture<LocationResponse>) {
        map[player.uniqueId] = future
        val output = ByteStreams.newDataOutput(32)
        output.writeUTF("portals:location")
        output.write(UUIDUtils.getBytesFromUUID(player.uniqueId))
        player.server.sendData("BungeeCord", output.toByteArray())
    }
}
