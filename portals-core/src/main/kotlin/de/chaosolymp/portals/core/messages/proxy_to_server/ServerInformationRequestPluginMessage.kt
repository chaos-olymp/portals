package de.chaosolymp.portals.core.messages.proxy_to_server

import de.chaosolymp.portals.annotations.messages.PluginMessage
import de.chaosolymp.portals.core.messages.AbstractPluginMessage

@PluginMessage("portals:p2s_server_information")
data class ServerInformationRequestPluginMessage(val proxyPluginVersion: String, val requestTimestamp: Long) : AbstractPluginMessage()