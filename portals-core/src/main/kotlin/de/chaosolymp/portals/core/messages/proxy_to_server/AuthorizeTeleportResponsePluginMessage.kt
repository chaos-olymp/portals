package de.chaosolymp.portals.core.messages.proxy_to_server

import de.chaosolymp.portals.annotations.messages.PluginMessage
import de.chaosolymp.portals.core.messages.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:p2s_authorize_teleport")
data class AuthorizeTeleportResponsePluginMessage(val uuid: UUID, val world: String, val x: Int, val y: Int, val z: Int): AbstractPluginMessage()