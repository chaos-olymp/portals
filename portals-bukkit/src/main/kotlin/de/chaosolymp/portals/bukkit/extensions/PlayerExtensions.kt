package de.chaosolymp.portals.bukkit.extensions

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.core.messages.AbstractPluginMessage
import de.chaosolymp.portals.core.messages.generated.serialize
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

@Suppress("UnstableApiUsage")
fun Player.sendPluginMessage(plugin: Plugin, message: AbstractPluginMessage) {
    val output = ByteStreams.newDataOutput()
    serialize(message, output)
    this.sendPluginMessage(plugin, "BungeeCord", output.toByteArray())
}