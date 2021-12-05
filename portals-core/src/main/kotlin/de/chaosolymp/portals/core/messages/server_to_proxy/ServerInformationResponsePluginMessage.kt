package de.chaosolymp.portals.core.messages.server_to_proxy

import de.chaosolymp.portals.annotations.messages.PluginMessage
import de.chaosolymp.portals.core.messages.AbstractPluginMessage

@PluginMessage("portals:s2p_server_information")
data class ServerInformationResponsePluginMessage(val pluginVersion: String, val timestamp: Long): AbstractPluginMessage()