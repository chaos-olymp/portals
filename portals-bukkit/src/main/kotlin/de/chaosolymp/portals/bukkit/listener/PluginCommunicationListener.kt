package de.chaosolymp.portals.bukkit.listener

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.bukkit.BukkitPlugin
import de.chaosolymp.portals.core.UUIDUtils
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

class PluginCommunicationListener(private val plugin: BukkitPlugin) : PluginMessageListener {

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if(channel == "BungeeCord" || channel == "bungeecord:main") {
            val input = ByteStreams.newDataInput(message)
            val subChannel = input.readUTF()
            if(subChannel == "portals:location") {
                val uuidBuffer = ByteArray(16)
                input.readFully(uuidBuffer)
                val uuid = UUIDUtils.getUUIDFromBytes(uuidBuffer)
                plugin.server.getPlayer(uuid)?.let {
                    val worldName = it.location.world!!.name
                    val output = ByteStreams.newDataOutput(33 + worldName.length)
                    output.write(uuidBuffer) // 16 byte
                    output.writeBoolean(this.plugin.canCreatePortal(it)) // 1 byte
                    output.writeUTF(worldName) // 4 byte + worldName.length
                    output.writeInt(it.location.x.toInt()) // 4 byte
                    output.writeInt(it.location.y.toInt()) // 4 byte
                    output.writeInt(it.location.z.toInt()) // 4 byte
                    it.sendPluginMessage(this.plugin, "BungeeCord", output.toByteArray())
                }
            } else if(subChannel == "portals:teleport") {
                val uuidBuffer = ByteArray(16)
                input.readFully(uuidBuffer)
                val uuid = UUIDUtils.getUUIDFromBytes(uuidBuffer)
                val world = input.readUTF()
                val x = input.readInt()
                val y = input.readInt()
                val z = input.readInt()

                this.plugin.server.getPlayer(uuid)?.let {
                        target -> {
                    this.plugin.server.getWorld(world).let {
                        val location = Location(it, x.toDouble(), y.toDouble(), z.toDouble())
                        target.teleport(location)
                    }
                }}
            }
        }
    }


}