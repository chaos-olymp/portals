package de.chaosolymp.portals.bukkit

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder

class ExceptionHandler(private val plugin: BukkitPlugin) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        val deeprobin = plugin.server.onlinePlayers.firstOrNull { player -> player.uniqueId.toString() == "375e2a8d-ab90-4601-adb1-23acafbd0c55" }
        plugin.logger.severe("Uncaught exception in Thread ${t.id}/${t.name} of type ${e.javaClass.name}: ${e.stackTraceToString()}")
        deeprobin?.spigot()?.sendMessage(*ComponentBuilder()
            .append("SERVER -")
            .color(ChatColor.RED)
            .append("SEVERE")
            .color(ChatColor.DARK_RED)
            .append("/portals ")
            .color(ChatColor.BLUE)
            .append("An exception occurred (Thread ${t.id}/${t.name} - ${e.javaClass.name}) - ${e.message ?: e.localizedMessage } - See console for stack trace")
            .color(ChatColor.RED)
            .create())
    }
}