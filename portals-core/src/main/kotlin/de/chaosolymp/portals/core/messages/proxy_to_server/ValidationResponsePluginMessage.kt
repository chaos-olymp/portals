package de.chaosolymp.portals.core.messages.proxy_to_server

import de.chaosolymp.portals.annotations.messages.PluginMessage
import de.chaosolymp.portals.core.messages.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:p2s_validate")
data class ValidationResponsePluginMessage(val uuid: UUID, val worldName: String, val x: Int, val y: Int, val z: Int, val valid: Boolean): AbstractPluginMessage()