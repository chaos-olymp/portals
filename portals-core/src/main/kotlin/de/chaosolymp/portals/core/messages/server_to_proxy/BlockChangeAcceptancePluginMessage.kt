package de.chaosolymp.portals.core.messages.server_to_proxy

import de.chaosolymp.portals.annotations.messages.PluginMessage
import de.chaosolymp.portals.core.messages.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:s2p_block_change_accepted")
data class BlockChangeAcceptancePluginMessage(val uuid: UUID): AbstractPluginMessage()