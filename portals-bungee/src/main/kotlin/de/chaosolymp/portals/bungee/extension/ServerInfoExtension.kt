package de.chaosolymp.portals.bungee.extension

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.bungee.DebugMessenger
import de.chaosolymp.portals.core.message.AbstractPluginMessage
import de.chaosolymp.portals.core.message.generated.serialize
import net.md_5.bungee.api.config.ServerInfo

@Suppress("UnstableApiUsage")
fun ServerInfo.sendData(message: AbstractPluginMessage) {
    val out = ByteStreams.newDataOutput()
    serialize(message, out)
    sendData("BungeeCord", out.toByteArray())

    DebugMessenger.verbose("Plugin Messaging", "Wrote outgoing plugin message $message")
}