package de.chaosolymp.portals.core.messages.server_to_proxy

import de.chaosolymp.portals.annotations.messages.PluginMessage
import de.chaosolymp.portals.core.messages.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:s2p_location")
data class LocationResponsePluginMessage(val uuid: UUID, val canCreatePortal: Boolean, val world: String, val x: Int, val y: Int, val z: Int): AbstractPluginMessage()