package de.chaosolymp.portals.core.message.proxy_to_server

import de.chaosolymp.portals.annotations.message.PluginMessage
import de.chaosolymp.portals.core.message.AbstractPluginMessage
import java.util.*

@PluginMessage("portals:p2s_block_change")
data class BlockChangeRequestPluginMessage(val uuid: UUID): AbstractPluginMessage()