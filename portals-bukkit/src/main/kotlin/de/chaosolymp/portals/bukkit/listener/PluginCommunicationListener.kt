package de.chaosolymp.portals.bukkit.listener

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.bukkit.BukkitPlugin
import de.chaosolymp.portals.core.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.messaging.PluginMessageListener

class PluginCommunicationListener(private val plugin: BukkitPlugin) : PluginMessageListener {
    @Suppress("UnstableApiUsage")
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel == "BungeeCord" || channel == "bungeecord:main") {
            val input = ByteStreams.newDataInput(message)
            when (input.readUTF()) {
                IDENTIFIER_LOCATION -> {
                    val uuidBuffer = ByteArray(16)
                    input.readFully(uuidBuffer)
                    val uuid = UUIDUtils.getUUIDFromBytes(uuidBuffer)
                    plugin.server.getPlayer(uuid)?.let {
                        val worldName = it.location.world!!.name
                        val output = ByteStreams.newDataOutput(53 + worldName.length)

                        output.writeUTF(IDENTIFIER_LOCATION) // 4 byte + length
                        output.write(uuidBuffer) // 16 byte
                        output.writeBoolean(this.plugin.canCreatePortal(it)) // 1 byte
                        output.writeUTF(worldName) // 4 byte + worldName.length
                        output.writeInt(it.location.x.toInt()) // 4 byte
                        output.writeInt(it.location.y.toInt()) // 4 byte
                        output.writeInt(it.location.z.toInt()) // 4 byte

                        it.sendPluginMessage(this.plugin, "BungeeCord", output.toByteArray())
                    }
                }
                IDENTIFIER_AUTHORIZE_TELEPORT -> {
                    val uuidBuffer = ByteArray(16)
                    input.readFully(uuidBuffer)
                    val uuid = UUIDUtils.getUUIDFromBytes(uuidBuffer)
                    val world = input.readUTF()
                    val x = input.readInt()
                    val y = input.readInt()
                    val z = input.readInt()

                    val target = this.plugin.server.getPlayer(uuid)

                        this.plugin.server.getWorld(world).let {
                            val location = Location(it, x.toDouble(), y.toDouble(), z.toDouble())
                            target?.teleport(location) ?: plugin.pendingTeleports.add(Pair(uuid, location))
                        }

                }
                IDENTIFIER_BLOCK_CHANGE -> {
                    val uuidBuffer = ByteArray(16)
                    input.readFully(uuidBuffer)
                    val uuid = UUIDUtils.getUUIDFromBytes(uuidBuffer)
                    this.plugin.server.getPlayer(uuid)?.let {
                        val blockLocation = it.player!!.location.subtract(0.0, 1.0, 0.0)
                        it.player!!.location.world!!.getBlockAt(blockLocation).setType(Material.END_PORTAL, false)
                        val output = ByteStreams.newDataOutput(49)
                        output.writeUTF(IDENTIFIER_BLOCK_CHANGE_ACCEPTED) // 4 byte + length
                        output.write(uuidBuffer) // 16 byte
                    }
                }
                IDENTIFIER_BLOCK_DESTROY -> {
                    val worldName = input.readUTF()
                    val x = input.readInt()
                    val y = input.readInt()
                    val z = input.readInt()

                    val chunkX = x shr 4
                    val chunkZ = z shr 4

                    val world = plugin.server.getWorld(worldName)!!
                    val chunk = world.getChunkAt(chunkX, chunkZ)

                    val loaded = chunk.isLoaded
                    if(!loaded) {
                        chunk.load(true)
                    }

                    val block = world.getBlockAt(x, y, z)
                    val stack = ItemStack(block.type, 1)
                    block.type = Material.AIR

                    world.dropItem(Location(world, x.toDouble(), y.toDouble(), z.toDouble()), stack)

                    if(!loaded) {
                        chunk.unload(true)
                    }

                }
                IDENTIFIER_VALIDATE_RESPONSE -> {
                    val uuidBuffer = ByteArray(16)
                    input.readFully(uuidBuffer)
                    val uuid = UUIDUtils.getUUIDFromBytes(uuidBuffer)
                    val worldName = input.readUTF()
                    val x = input.readInt()
                    val y = input.readInt()
                    val z = input.readInt()
                    val valid = input.readBoolean()


                    plugin.portalRequestMap[Pair(worldName, Triple(x, y, z))]?.complete(valid)
                }
            }
        }
    }


}