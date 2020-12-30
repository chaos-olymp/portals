package de.chaosolymp.portals.bungee.listener

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.bungee.BungeePlugin
import de.chaosolymp.portals.core.*
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.*
import java.util.concurrent.CompletableFuture

class PluginMessageListener(val plugin: BungeePlugin) : Listener {

    private val map = mutableMapOf<UUID, CompletableFuture<LocationResponse>>()

    private val blockChangeMap = mutableMapOf<UUID, CompletableFuture<Void>>()

    @Suppress("UnstableApiUsage")
    @EventHandler
    fun handlePluginMessage(event: PluginMessageEvent) {
        if (event.tag == "BungeeCord" || event.tag == "bungeecord:main") {
            val input = ByteStreams.newDataInput(event.data)
            val subChannel = input.readUTF()
            if (subChannel == IDENTIFIER_LOCATION) {
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
            } else if (subChannel == IDENTIFIER_AUTHORIZE_TELEPORT) {
                val uuidArray = ByteArray(16)
                input.readFully(uuidArray)
                val uuid = UUIDUtils.getUUIDFromBytes(uuidArray)
                val world = input.readUTF()
                val x = input.readInt()
                val y = input.readInt()
                val z = input.readInt()

                val server = this.plugin.proxy.getPlayer(uuid).server.info.name

                val id = this.plugin.portalManager.getPortalIdAt(server, world, x, y, z)
                if (id != null) {
                    val link = this.plugin.portalManager.getPortalLink(id)
                    val portal = this.plugin.portalManager.getPortal(link)

                    val serverInfo = this.plugin.proxy.getServerInfo(portal?.server)

                    if (portal?.server != server) {
                        this.plugin.proxy.getPlayer(uuid).connect(serverInfo)
                    }

                    val out = ByteStreams.newDataOutput(48 + portal!!.world.length)

                    out.writeUTF(IDENTIFIER_AUTHORIZE_TELEPORT) // 4 byte + length
                    out.write(uuidArray) // 16 byte
                    out.writeUTF(portal.world) // 34 byte + portal.world.length
                    out.writeInt(portal.x) // 4 byte
                    out.writeInt(portal.y) // 4 byte
                    out.writeInt(portal.z) // 4 byte

                    serverInfo.sendData("BungeeCord", out.toByteArray())

                    println("Y")
                } else {
                    this.plugin.logger.warning("${event.sender.socketAddress}: caught invalid teleportation")
                }
            } else if (subChannel == IDENTIFIER_BLOCK_CHANGE_ACCEPTED) {
                val uuidArray = ByteArray(16)
                input.readFully(uuidArray)
                val uuid = UUIDUtils.getUUIDFromBytes(uuidArray)
                if (blockChangeMap.containsKey(uuid)) {
                    blockChangeMap[uuid]?.complete(null)
                    blockChangeMap.remove(uuid)
                } else {
                    this.plugin.proxy.logger.warning("${event.sender.socketAddress} sent block change response for non-requested uuid $uuid.")
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    fun requestLocation(player: ProxiedPlayer, future: CompletableFuture<LocationResponse>) {
        map[player.uniqueId] = future
        val output = ByteStreams.newDataOutput(36)

        output.writeUTF(IDENTIFIER_LOCATION) // 4 byte + length
        output.write(UUIDUtils.getBytesFromUUID(player.uniqueId)) // 16 byte

        player.server.sendData("BungeeCord", output.toByteArray())
    }

    @Suppress("UnstableApiUsage")
    fun sendBlockChange(player: ProxiedPlayer, future: CompletableFuture<Void>) {
        blockChangeMap[player.uniqueId] = future
        val output = ByteStreams.newDataOutput(36)

        output.writeUTF(IDENTIFIER_BLOCK_CHANGE) // 4 byte + length
        output.write(UUIDUtils.getBytesFromUUID(player.uniqueId)) // 16 byte

        player.server.sendData("BungeeCord", output.toByteArray())
    }
}
