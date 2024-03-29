package de.chaosolymp.portals.bungee.extension

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.core.message.AbstractPluginMessage
import de.chaosolymp.portals.core.message.generated.serialize
import net.md_5.bungee.api.connection.Server

@Suppress("UnstableApiUsage")
fun Server.sendData(message: AbstractPluginMessage) {
    val out = ByteStreams.newDataOutput()
    serialize(message, out)
    sendData("BungeeCord", out.toByteArray())
}