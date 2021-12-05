package de.chaosolymp.portals.core.message.proxy_to_server

import de.chaosolymp.portals.annotations.message.PluginMessage
import de.chaosolymp.portals.core.message.AbstractPluginMessage

@PluginMessage("portals:p2s_server_information")
data class ServerInformationRequestPluginMessage(val proxyPluginVersion: String, val requestTimestamp: Long) : AbstractPluginMessage()