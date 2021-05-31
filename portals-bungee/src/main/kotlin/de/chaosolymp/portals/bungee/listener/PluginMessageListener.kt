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
                    plugin.logger.info("link = $link")
                    val portal = this.plugin.portalManager.getPortal(link)
                    plugin.logger.info("portal = $portal")

                    val serverInfo = this.plugin.proxy.getServerInfo(portal?.server)
                    plugin.logger.info("serverInfo = $serverInfo")
                    if (portal?.server != server) {
                        plugin.logger.info("connecting")
                        this.plugin.proxy.getPlayer(uuid).connect(serverInfo)
                    } else {
                        plugin.logger.info("Cannot connect, the player is on the same server")
                    }

                    val out = ByteStreams.newDataOutput(48 + portal!!.world.length)

                    out.writeUTF(IDENTIFIER_AUTHORIZE_TELEPORT) // 4 byte + length
                    out.write(uuidArray) // 16 byte
                    out.writeUTF(portal.world) // 34 byte + portal.world.length
                    out.writeInt(portal.x) // 4 byte
                    out.writeInt(portal.y) // 4 byte
                    out.writeInt(portal.z) // 4 byte

                    serverInfo.sendData("BungeeCord", out.toByteArray())
                    plugin.logger.info("Sent plugin message")
                    if (portal.server != server) {
                        this.plugin.proxy.getPlayer(uuid).connect(serverInfo)
                    }
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
            } else if(subChannel == IDENTIFIER_VALIDATE) {
                val uuidBuffer = ByteArray(16)
                input.readFully(uuidBuffer)
                val uuid = UUIDUtils.getUUIDFromBytes(uuidBuffer)

                val worldName = input.readUTF()
                val x = input.readInt()
                val y = input.readInt()
                val z = input.readInt()

                val serverInfo = this.plugin.proxy.getPlayer(uuid).server.info

                val valid = plugin.portalManager.getPortalIdAt(serverInfo.name, worldName, x, y, z) != null

                val out = ByteStreams.newDataOutput(49 + worldName.length)

                out.writeUTF(IDENTIFIER_VALIDATE_RESPONSE) // 4 byte + length
                out.write(uuidBuffer) // 16 byte
                out.writeUTF(worldName) // 34 byte + portal.world.length
                out.writeInt(x) // 4 byte
                out.writeInt(y) // 4 byte
                out.writeInt(z) // 4 byte
                out.writeBoolean(valid) // 1 byte

                serverInfo.sendData("BungeeCord", out.toByteArray())
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

    @Suppress("UnstableApiUsage")
    fun sendBlockDestroy(server: String, world: String, x: Int, y: Int, z: Int) {
        val output = ByteStreams.newDataOutput(20 + IDENTIFIER_BLOCK_DESTROY.length + world.length)

        output.writeUTF(IDENTIFIER_BLOCK_DESTROY) // 4 byte + length
        output.writeUTF(world) // 4 byte + length
        output.writeInt(x) // 4 byte
        output.writeInt(y) // 4 byte
        output.writeInt(z) // 4 byte

        plugin.proxy.servers[server]!!.sendData("BungeeCord", output.toByteArray())
    }
}
