package de.chaosolymp.portals.core.message.server_to_proxy

import de.chaosolymp.portals.annotations.message.PluginMessage
import de.chaosolymp.portals.core.message.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:s2p_remove_portal")
data class RemovePortalPluginMessage(val player: UUID, val world: String, val x: Int, val y: Int, val z: Int) : AbstractPluginMessage()