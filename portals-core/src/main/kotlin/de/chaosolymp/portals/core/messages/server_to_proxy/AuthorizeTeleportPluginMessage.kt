package de.chaosolymp.portals.core.messages.server_to_proxy

import de.chaosolymp.portals.annotations.messages.PluginMessage
import de.chaosolymp.portals.core.messages.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:authorize_teleport")
data class AuthorizeTeleportPluginMessage(val uuid: UUID, val world: String, val x: Int, val y: Int, val z: Int): AbstractPluginMessage()