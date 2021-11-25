package de.chaosolymp.portals.core.messages.proxy_to_server

import de.chaosolymp.portals.annotations.messages.PluginMessage
import de.chaosolymp.portals.core.messages.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:p2s_location")
data class RequestLocationPluginMessage(val uuid: UUID): AbstractPluginMessage()