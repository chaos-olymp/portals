package de.chaosolymp.portals.bukkit.extensions

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.core.message.AbstractPluginMessage
import de.chaosolymp.portals.core.message.generated.serialize
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@Suppress("UnstableApiUsage")
fun Player.sendPluginMessage(plugin: Plugin, message: AbstractPluginMessage) {
    val output = ByteStreams.newDataOutput()
    serialize(message, output)
    sendPluginMessage(plugin, "BungeeCord", output.toByteArray())
}