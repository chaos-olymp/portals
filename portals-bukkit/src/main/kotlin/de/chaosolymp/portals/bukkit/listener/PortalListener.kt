package de.chaosolymp.portals.bukkit.listener

import de.chaosolymp.portals.bukkit.BukkitPlugin
import de.chaosolymp.portals.bukkit.PORTAL_MATERIAL
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.util.*

class PortalListener(private val plugin: BukkitPlugin) : Listener {

    private val joinTimeMap = mutableMapOf<UUID, Long>()

    private val countDownTickMap = mutableMapOf<Player, Int>()
    private val teleportTicks = 6

    init {
        scheduleCountdownTask()
        scheduleAppearanceTask()
    }

    private fun scheduleCountdownTask() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            for(player in countDownTickMap.keys) {
                val block = player.location.block
                // Sneaking or quitting the game, cancels the task
                if (!player.isSneaking || !player.isOnline || player.location.block.type != PORTAL_MATERIAL) {
                    countDownTickMap.remove(player)
                    return@Runnable
                }

                // Send action bar message to player
                val greenBlocks = teleportTicks - countDownTickMap[player]!!
                val whiteBlocks = teleportTicks - greenBlocks
                val component = ComponentBuilder()
                    .append("\u2588".repeat(greenBlocks))
                    .color(ChatColor.GREEN)
                    .append("\u2588".repeat(whiteBlocks))
                    .color(ChatColor.WHITE)
                    .create()

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *component)

                if (countDownTickMap[player]!! == teleportTicks) {
                    countDownTickMap.remove(player)
                    plugin.teleport(player, block)
                    return@Runnable
                }
                countDownTickMap[player] = countDownTickMap[player]!!.inc()
            }

        }, 0L, 7L)
    }

    private fun scheduleAppearanceTask() {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            for (player in plugin.server.onlinePlayers) {
                // Get block at moving-to location
                val world = player.location.world ?: continue
                val block = world.getBlockAt(player.location)

                // Only handle blocks with typeof PORTAL_MATERIAL
                if (block.type != PORTAL_MATERIAL) continue

                if(!countDownTickMap.containsKey(player)) {
                    // Show particles
                    plugin.handlePortalAppearance(player)
                }
            }
        }, 0L, 1L)
    }

    @EventHandler
    fun handleSneakInPortal(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return

        val player = event.player

        // Get block at moving-to location
        val world = event.player.location.world ?: return
        val block = world.getBlockAt(event.player.location)

        // Only handle blocks with typeof PORTAL_MATERIAL
        if (block.type != PORTAL_MATERIAL) return

        if (!countDownTickMap.containsKey(player)) {
            countDownTickMap[player] = 0
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun handleBreakPortal(event: BlockBreakEvent) {
        // Disallow portal block breaking
        if (event.block.type != PORTAL_MATERIAL) return

        event.isCancelled = true
    }

    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        for (pendingTeleport in plugin.pendingTeleports) {
            if (pendingTeleport.first != event.player.uniqueId) continue

            event.player.teleport(pendingTeleport.second)
            plugin.pendingTeleports.remove(pendingTeleport)
            joinTimeMap[pendingTeleport.first] = System.currentTimeMillis()
            return
        }
    }
}
