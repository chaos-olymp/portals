package de.chaosolymp.portals.core.message.server_to_proxy

import de.chaosolymp.portals.annotations.message.PluginMessage
import de.chaosolymp.portals.core.message.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:s2p_block_change_accepted")
data class BlockChangeAcceptancePluginMessage(val uuid: UUID): AbstractPluginMessage()