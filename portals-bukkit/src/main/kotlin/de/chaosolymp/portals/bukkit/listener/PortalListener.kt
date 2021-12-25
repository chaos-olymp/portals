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

    private val coolDownTickMap = mutableMapOf<Player, Int>()
    private val taskMap = mutableMapOf<Player, Int>()

    @EventHandler
    fun handleSneakInPortal(event: PlayerToggleSneakEvent) {
        if(!event.isSneaking) return

        // Get block at moving-to location
        val world = event.player.location.world ?: return
        val block = world.getBlockAt(event.player.location)

        // Only handle blocks with typeof PORTAL_MATERIAL
        if(block.type != PORTAL_MATERIAL) return

        val player = event.player

        // Show particles
        plugin.handlePortalAppearance(player)

        val teleportTicks = 6
        val tickRunnable = Runnable {
            if(!coolDownTickMap.containsKey(player)) {
                coolDownTickMap[player] = 0
            }

            val newBlockLocation = world.getBlockAt(player.location).location

            // Sneaking or quitting the game, cancels the task
            if(!player.isSneaking || !player.isOnline || !(newBlockLocation.x == block.location.x && newBlockLocation.y == block.location.y && newBlockLocation.z == block.location.z)) {
                taskMap[player]?.let {
                    plugin.server.scheduler.cancelTask(it)
                }
                taskMap.remove(player)
                coolDownTickMap.remove(player)
                return@Runnable
            }

            // Send action bar message to player
            val greenBlocks = teleportTicks - coolDownTickMap[player]!!
            val whiteBlocks = teleportTicks - greenBlocks
            val component = ComponentBuilder()
                .append("\u2588".repeat(greenBlocks))
                .color(ChatColor.GREEN)
                .append("\u2588".repeat(whiteBlocks))
                .color(ChatColor.WHITE)
                .create()

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *component)

            if(coolDownTickMap[player]!! == teleportTicks) {
                val task = taskMap[player]!!
                taskMap.remove(player)
                coolDownTickMap.remove(player)
                plugin.teleport(player, block)
                plugin.server.scheduler.cancelTask(task)
                return@Runnable
            }
            coolDownTickMap[player] = coolDownTickMap[player]!!.inc()
        }
        val scheduledTask = plugin.server.scheduler.runTaskTimer(plugin, tickRunnable, 0L, 10L)
        taskMap[player] = scheduledTask.taskId
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun handleBreakPortal(event: BlockBreakEvent) {
        // Disallow portal block breaking
        if (event.block.type != PORTAL_MATERIAL) return

        plugin.removePortal(event.player, event.block.location)
    }

    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        for(pendingTeleport in plugin.pendingTeleports) {
            if (pendingTeleport.first != event.player.uniqueId) continue

            event.player.teleport(pendingTeleport.second)
            plugin.pendingTeleports.remove(pendingTeleport)
            joinTimeMap[pendingTeleport.first] = System.currentTimeMillis()
            return
        }
    }
}
