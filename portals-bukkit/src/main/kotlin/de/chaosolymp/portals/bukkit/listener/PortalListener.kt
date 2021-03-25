package de.chaosolymp.portals.bukkit.listener

import de.chaosolymp.portals.bukkit.BukkitPlugin
import de.chaosolymp.portals.bukkit.PORTAL_MATERIAL
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.util.*

class PortalListener(private val plugin: BukkitPlugin) : Listener {

    private val cooldown = 3 * 1000 // In milliseconds (1 * 3000 = 3 seconds)
    private val joinTimeMap = mutableMapOf<UUID, Long>()

    @EventHandler
    fun handleEnterPortal(event: PlayerMoveEvent) {
        event.to?.let {
            val world = it.world!!
            val block = world.getBlockAt(it)
            if(block.type == PORTAL_MATERIAL) {
                plugin.handlePortalAppearance(event.player)
                if(event.player.isSneaking) {
                    plugin.teleport(event.player, block)
                }
            }
        }
    }

    @EventHandler
    fun handleJoin(event: PlayerJoinEvent) {
        plugin.pendingTeleports.forEach {
            if(it.first == event.player.uniqueId) {
                event.player.teleport(it.second)
                plugin.pendingTeleports.remove(it)
                joinTimeMap[it.first] = System.currentTimeMillis()
                return
            }
        }
    }

    @EventHandler
    fun handleSneakToggle(event: PlayerToggleSneakEvent) {
        if((joinTimeMap[event.player.uniqueId]?.plus(cooldown))!! > System.currentTimeMillis()) {
            event.player.sendTitle("", "Cooldown ...", 500, 500, 500)
            return
        }
        if(event.isSneaking) {
            val location = event.player.location
            val block = location.world!!.getBlockAt(location)
            if(block.type == PORTAL_MATERIAL) {
                plugin.teleport(event.player, block)
            }
        }
    }

}
