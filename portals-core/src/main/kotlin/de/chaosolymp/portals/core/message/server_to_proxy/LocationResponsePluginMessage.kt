package de.chaosolymp.portals.core.message.server_to_proxy

import de.chaosolymp.portals.annotations.message.PluginMessage
import de.chaosolymp.portals.core.message.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:s2p_location")
data class LocationResponsePluginMessage(val uuid: UUID, val canCreatePortal: Boolean, val world: String, val x: Int, val y: Int, val z: Int, val yaw: Float, val pitch: Float): AbstractPluginMessage()