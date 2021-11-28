package de.chaosolymp.portals.bukkit.listener

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.bukkit.BukkitPlugin
import de.chaosolymp.portals.bukkit.extensions.sendPluginMessage
import de.chaosolymp.portals.core.messages.generated.deserialize
import de.chaosolymp.portals.core.messages.proxy_to_server.*
import de.chaosolymp.portals.core.messages.server_to_proxy.AuthorizeTeleportRequestPluginMessage
import de.chaosolymp.portals.core.messages.server_to_proxy.BlockChangeAcceptancePluginMessage
import de.chaosolymp.portals.core.messages.server_to_proxy.LocationResponsePluginMessage
import de.chaosolymp.portals.core.messages.server_to_proxy.ServerInformationResponsePluginMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.messaging.PluginMessageListener
import java.time.Instant

class PluginCommunicationListener(private val plugin: BukkitPlugin) : PluginMessageListener {
    @Suppress("UnstableApiUsage")
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (!(channel == "BungeeCord" || channel == "bungeecord:main")) return

        // Deserialize input to message (see AbstractPluginMessage)
        val input = ByteStreams.newDataInput(message)
        val deserialized = deserialize(input)

        // Log incoming message for debugging purposes
        if (deserialized != null) {
            plugin.logger.info("Incoming plugin message of type ${deserialized.javaClass} - $deserialized")
        }

        when (deserialized) {
            is RequestLocationPluginMessage -> {
                val targetPlayer = plugin.server.getPlayer(deserialized.uuid) ?: return
                val worldName = targetPlayer.world.name

                val outgoingMessage = LocationResponsePluginMessage(
                    deserialized.uuid,
                    plugin.canCreatePortal(targetPlayer),
                    worldName,
                    targetPlayer.location.x.toInt(),
                    targetPlayer.location.y.toInt(),
                    targetPlayer.location.z.toInt()
                )

                player.sendPluginMessage(
                    this.plugin, outgoingMessage
                )
                plugin.logger.info("Wrote outgoing message: $outgoingMessage")
            }
            is AuthorizeTeleportRequestPluginMessage -> {
                val targetPlayer = plugin.server.getPlayer(deserialized.uuid)
                val world = plugin.server.getWorld(deserialized.world)

                val location =
                    Location(world, deserialized.x.toDouble(), deserialized.y.toDouble(), deserialized.z.toDouble())
                targetPlayer?.teleport(location) ?: plugin.pendingTeleports.add(Pair(deserialized.uuid, location))
            }
            is BlockChangeRequestPluginMessage -> {
                val targetPlayer = plugin.server.getPlayer(deserialized.uuid) ?: return
                val blockLocation = targetPlayer.location
                val block = blockLocation.world!!.getBlockAt(blockLocation)
                block.setType(Material.END_PORTAL, false)

                val outgoingMessage = BlockChangeAcceptancePluginMessage(deserialized.uuid)

                player.sendPluginMessage(this.plugin, outgoingMessage)
                plugin.logger.info("Wrote outgoing message: $outgoingMessage")
            }
            is BlockDestroyRequestPluginMessage -> {
                val chunkX = deserialized.x shr 4
                val chunkZ = deserialized.z shr 4

                val world = plugin.server.getWorld(deserialized.world) ?: return
                val chunk = world.getChunkAt(chunkX, chunkZ)

                val loaded = chunk.isLoaded
                if (!loaded) {
                    chunk.load(true)
                }

                val block = world.getBlockAt(deserialized.x, deserialized.y, deserialized.z)
                val stack = ItemStack(block.type, 1)
                block.type = Material.AIR

                world.dropItem(
                    Location(
                        world,
                        deserialized.x.toDouble(),
                        deserialized.y.toDouble(),
                        deserialized.z.toDouble()
                    ), stack
                )

                if (!loaded) {
                    chunk.unload(true)
                }
            }
            is ValidationResponsePluginMessage -> {
                plugin.portalRequestMap[Pair(
                    deserialized.worldName,
                    Triple(deserialized.x, deserialized.y, deserialized.z)
                )]?.complete(deserialized.valid)
            }
            is ServerInformationRequestPluginMessage -> {
                val outgoingMessage = ServerInformationResponsePluginMessage(
                    plugin.description.version,
                    Instant.now().toEpochMilli(),
                    plugin.exceptionHandler.exceptionCount
                )
                player.sendPluginMessage(
                    this.plugin, outgoingMessage
                )
                plugin.logger.info("Wrote outgoing message: $outgoingMessage")
            }
            else -> plugin.logger.warning("Unknown incoming plugin message")
        }
    }
}