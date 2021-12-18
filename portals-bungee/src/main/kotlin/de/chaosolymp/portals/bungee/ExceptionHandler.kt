package de.chaosolymp.portals.bungee

import de.chaosolymp.portals.bungee.extension.sendMessage
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import java.io.PrintWriter
import java.io.StringWriter

class ExceptionHandler(val plugin: BungeePlugin) : Thread.UncaughtExceptionHandler {
    internal var exceptionCount = 0

    override fun uncaughtException(t: Thread, e: Throwable) {
        val stringWriter = StringWriter()
        e.printStackTrace(PrintWriter(stringWriter))
        if(!stringWriter.toString().contains("de.chaosolymp.portals")) return

        val deeprobin = plugin.proxy.players.firstOrNull { player -> player.uniqueId.toString() == "375e2a8d-ab90-4601-adb1-23acafbd0c55" }
        plugin.logger.severe("Uncaught exception in Thread ${t.id}/${t.name} of type ${e.javaClass.name}: ${e.stackTraceToString()}")
        deeprobin?.sendMessage(ComponentBuilder()
            .append("PROXY - ")
            .color(ChatColor.RED)
            .append("SEVERE")
            .color(ChatColor.DARK_RED)
            .append("/portals ")
            .color(ChatColor.DARK_RED)
            .append("An exception occurred (Thread ${t.id}/${t.name} - ${e.javaClass.name}) - ${e.message ?: e.localizedMessage } - See console for stack trace")
            .color(ChatColor.RED)
            .create())

        exceptionCount++
        e.printStackTrace()
    }
}