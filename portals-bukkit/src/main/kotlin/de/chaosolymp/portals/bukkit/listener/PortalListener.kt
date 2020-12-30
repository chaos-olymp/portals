package de.chaosolymp.portals.bukkit.listener

import de.chaosolymp.portals.bukkit.BukkitPlugin
import de.chaosolymp.portals.bukkit.PORTAL_MATERIAL
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleSneakEvent

class PortalListener(private val plugin: BukkitPlugin) : Listener {

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
                return
            }
        }
    }

    @EventHandler
    fun handleSneakToggle(event: PlayerToggleSneakEvent) {
        if(event.isSneaking) {
            val location = event.player.location
            val block = location.world!!.getBlockAt(location)
            if(block.type == PORTAL_MATERIAL) {
                plugin.teleport(event.player, block)
            }
        }
    }

}
