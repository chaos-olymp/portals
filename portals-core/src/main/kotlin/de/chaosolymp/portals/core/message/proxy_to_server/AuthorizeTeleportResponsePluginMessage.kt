package de.chaosolymp.portals.core.message.proxy_to_server

import de.chaosolymp.portals.annotations.message.PluginMessage
import de.chaosolymp.portals.core.message.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:p2s_authorize_teleport")
data class AuthorizeTeleportResponsePluginMessage(val uuid: UUID, val world: String, val x: Int, val y: Int, val z: Int, val yaw: Float, val pitch: Float): AbstractPluginMessage()