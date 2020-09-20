package de.chaosolymp.portals.bukkit.listener

import de.chaosolymp.portals.bukkit.BukkitPlugin
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.util.*

class PortalListener(private val plugin: BukkitPlugin) : Listener {

    @EventHandler
    fun handleEnterPortal(event: PlayerMoveEvent) {
        event.to?.let {
            val world = it.world!!
            val block = world.getBlockAt(it)
            if(block.type == Material.END_PORTAL) {
                plugin.handlePortalAppearance(event.player)
                if(event.player.isSneaking) {
                    plugin.teleport(event.player, block)
                }
            }
        }
    }

    @EventHandler
    fun handleSneakToggle(event: PlayerToggleSneakEvent) {
        if(event.isSneaking) {
            val location = event.player.location
            val block = location.world!!.getBlockAt(location)
            if(block.type == Material.END_PORTAL) {
                plugin.teleport(event.player, block)
            }
        }
    }

}
