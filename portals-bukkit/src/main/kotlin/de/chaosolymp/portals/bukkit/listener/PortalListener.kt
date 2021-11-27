package de.chaosolymp.portals.bukkit.listener

import de.chaosolymp.portals.bukkit.BukkitPlugin
import de.chaosolymp.portals.bukkit.PORTAL_MATERIAL
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.util.*

class PortalListener(private val plugin: BukkitPlugin) : Listener {

    private val coolDown = 3 * 1000 // In milliseconds (1 * 3000 = 3 seconds)
    private val joinTimeMap = mutableMapOf<UUID, Long>()

    @EventHandler
    fun handleEnterPortal(event: PlayerMoveEvent) {
        val to = event.to
        if(event.to == null) return

        // Get block at moving-to location
        val world = to?.world ?: return
        val block = world.getBlockAt(to)

        // Only handle blocks with typeof PORTAL_MATERIAL
        if(block.type != PORTAL_MATERIAL) return

        // Show particles
        plugin.handlePortalAppearance(event.player)

        // Only teleport player if he's sneaking
        if(!event.player.isSneaking) return
        plugin.teleport(event.player, block)
    }

    @EventHandler
    fun handleBreakPortal(event: BlockBreakEvent) {
        // Disallow portal block breaking
        if (event.block.type != PORTAL_MATERIAL || !plugin.isValidPortal(event.player, event.block.location)) return

        event.isCancelled = true
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

    @EventHandler
    fun handleSneakToggle(event: PlayerToggleSneakEvent) {
        val joinTime = joinTimeMap[event.player.uniqueId]?.plus(coolDown)
        if(joinTime != null && joinTime > System.currentTimeMillis()) {
            event.player.sendTitle("", "Cooldown ...", 500, 500, 500)
            return
        }
        if(event.isSneaking) {
            val location = event.player.location
            val block = location.world!!.getBlockAt(location)
            if(block.type == PORTAL_MATERIAL) {
                plugin.teleport(event.player, block)
            }
        } else {
            joinTimeMap.remove(event.player.uniqueId)
        }
    }

}
