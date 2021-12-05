package de.chaosolymp.portals.core.message.server_to_proxy

import de.chaosolymp.portals.annotations.message.PluginMessage
import de.chaosolymp.portals.core.message.AbstractPluginMessage

@PluginMessage("portals:s2p_server_information")
data class ServerInformationResponsePluginMessage(val pluginVersion: String, val timestamp: Long): AbstractPluginMessage()