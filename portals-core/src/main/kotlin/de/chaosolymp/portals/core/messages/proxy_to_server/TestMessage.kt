package de.chaosolymp.portals.core.messages.proxy_to_server

import de.chaosolymp.portals.annotations.messages.PluginMessage

@PluginMessage("portals:test")
class TestMessage {
    val myTestField = "";
}