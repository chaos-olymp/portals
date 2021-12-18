package de.chaosolymp.portals.bukkit.listener

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.bukkit.BukkitPlugin
import de.chaosolymp.portals.bukkit.PORTAL_BASE_MATERIAL
import de.chaosolymp.portals.bukkit.PORTAL_MATERIAL
import de.chaosolymp.portals.bukkit.extensions.sendPluginMessage
import de.chaosolymp.portals.core.message.generated.deserialize
import de.chaosolymp.portals.core.message.proxy_to_server.*
import de.chaosolymp.portals.core.message.server_to_proxy.BlockChangeAcceptancePluginMessage
import de.chaosolymp.portals.core.message.server_to_proxy.LocationResponsePluginMessage
import de.chaosolymp.portals.core.message.server_to_proxy.RemovePortalPluginMessage
import de.chaosolymp.portals.core.message.server_to_proxy.ServerInformationResponsePluginMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.messaging.PluginMessageListener
import java.time.Instant

class PluginCommunicationListener(private val plugin: BukkitPlugin) : PluginMessageListener {
    @Suppress("UnstableApiUsage")
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        // Only handle BungeeCord or bungeecord:main
        if (!(channel == "BungeeCord" || channel == "bungeecord:main")) return

        // Deserialize input to message (see AbstractPluginMessage)
        val input = ByteStreams.newDataInput(message)
        val deserialized = deserialize(input)

        // Log incoming message for debugging purposes
        if (deserialized != null) {
            plugin.logger.info("Incoming plugin message of type ${deserialized.javaClass} - $deserialized")
        }

        when (deserialized) {
            is RequestLocationPluginMessage -> handleRequestLocationPluginMessage(player, deserialized)
            is AuthorizeTeleportResponsePluginMessage -> handleAuthorizeTeleportResponsePluginMessage(deserialized)
            is BlockChangeRequestPluginMessage -> handleBlockChangeRequestPluginMessage(player, deserialized)
            is BlockDestroyRequestPluginMessage -> handleBlockDestroyRequestPluginMessage(deserialized)
            is ValidationResponsePluginMessage -> handleValidationResponsePluginMessage(deserialized)
            is ServerInformationRequestPluginMessage -> handleServerInformationRequestPluginMessage(player)
            is CleanupRequestPluginMessage -> handleCleanupRequestPluginMessage(player, deserialized)
            else -> plugin.logger.warning("Unknown incoming plugin message")
        }
    }

    private fun handleCleanupRequestPluginMessage(player: Player, deserialized: CleanupRequestPluginMessage) {
        val world = plugin.server.getWorld(deserialized.world) ?: return
        val x = deserialized.x
        val y = deserialized.y
        val z = deserialized.z

        val block = world.getBlockAt(x, y, z)
        if(block.type == PORTAL_MATERIAL) return

        player.sendPluginMessage(plugin, RemovePortalPluginMessage(player.uniqueId, world.name, x, y, z))
    }

    private fun handleRequestLocationPluginMessage(player: Player, deserialized: RequestLocationPluginMessage) {
        val targetPlayer = plugin.server.getPlayer(deserialized.uuid) ?: return
        val worldName = targetPlayer.world.name

        val outgoingMessage = LocationResponsePluginMessage(
            deserialized.uuid,
            plugin.canCreatePortal(targetPlayer),
            worldName,
            targetPlayer.location.blockX,
            targetPlayer.location.blockY,
            targetPlayer.location.blockZ
        )

        player.sendPluginMessage(
            plugin, outgoingMessage
        )
        plugin.logger.info("Wrote outgoing message: $outgoingMessage")
    }

    private fun handleAuthorizeTeleportResponsePluginMessage(deserialized: AuthorizeTeleportResponsePluginMessage) {
        val targetPlayer = plugin.server.getPlayer(deserialized.uuid)
        val world = plugin.server.getWorld(deserialized.world)

        if (world == null) {
            plugin.logger.warning("World ${deserialized.world} does not exist - Aborting teleport")
            return
        }

        // Add 0.5 block offset because the player must spawn in the middle of the block
        // to remove damage due stucking in a block
        val x = deserialized.x.toDouble() + 0.5
        val z = deserialized.z.toDouble() + 0.5

        val y = deserialized.y.toDouble() + 1

        val location =
            Location(world, x, y, z)

        if (targetPlayer == null) {
            plugin.pendingTeleports.add(Pair(deserialized.uuid, location))
            plugin.logger.fine("Added player with UUID ${deserialized.uuid} to pending teleports")
            return
        }

        targetPlayer.teleport(location)
        plugin.logger.fine("Teleported player ${targetPlayer.name} (${targetPlayer.uniqueId}) to W: ${world.name} X: $x Y: $y Z: $z")
    }

    private fun handleBlockChangeRequestPluginMessage(player: Player, deserialized: BlockChangeRequestPluginMessage) {
        val targetPlayer = plugin.server.getPlayer(deserialized.uuid) ?: return
        val blockLocation = targetPlayer.location
        val block = blockLocation.world!!.getBlockAt(blockLocation)
        block.setType(Material.END_PORTAL, false)

        val outgoingMessage = BlockChangeAcceptancePluginMessage(deserialized.uuid)

        player.sendPluginMessage(plugin, outgoingMessage)
        plugin.logger.info("Wrote outgoing message: $outgoingMessage")
    }

    private fun handleBlockDestroyRequestPluginMessage(deserialized: BlockDestroyRequestPluginMessage) {
        val chunkX = deserialized.x shr 4
        val chunkZ = deserialized.z shr 4

        val world = plugin.server.getWorld(deserialized.world) ?: return
        val chunk = world.getChunkAt(chunkX, chunkZ)

        val loaded = chunk.isLoaded

        // Load chunk if it is not loaded, so we can access the chunk
        if (!loaded) {
            chunk.load(true)
        }

        val block = world.getBlockAt(deserialized.x, deserialized.y, deserialized.z)
        val stack = ItemStack(PORTAL_BASE_MATERIAL, 1)
        block.type = Material.AIR

        // Spawn PORTAL_BASE_MATERIAL item
        world.dropItem(
            Location(
                world,
                deserialized.x.toDouble(),
                deserialized.y.toDouble(),
                deserialized.z.toDouble()
            ), stack
        )

        // Unload chunk if it was previously unloaded
        if (!loaded) {
            chunk.unload(true)
        }
    }

    private fun handleValidationResponsePluginMessage(deserialized: ValidationResponsePluginMessage) {
        plugin.portalRequestMap[Pair(
            deserialized.worldName,
            Triple(deserialized.x, deserialized.y, deserialized.z)
        )]?.complete(deserialized.valid)
    }

    private fun handleServerInformationRequestPluginMessage(player: Player) {
        val outgoingMessage = ServerInformationResponsePluginMessage(
            plugin.description.version,
            Instant.now().toEpochMilli()
        )
        player.sendPluginMessage(
            plugin, outgoingMessage
        )
        plugin.logger.info("Wrote outgoing message: $outgoingMessage")
    }
}