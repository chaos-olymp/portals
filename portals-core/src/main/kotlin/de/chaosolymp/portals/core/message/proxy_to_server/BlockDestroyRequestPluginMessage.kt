package de.chaosolymp.portals.core.message.proxy_to_server

import de.chaosolymp.portals.annotations.message.PluginMessage
import de.chaosolymp.portals.core.message.AbstractPluginMessage

@PluginMessage("portals:p2s_block_destroy")
class BlockDestroyRequestPluginMessage(val world: String, val x: Int, val y: Int, val z: Int): AbstractPluginMessage()