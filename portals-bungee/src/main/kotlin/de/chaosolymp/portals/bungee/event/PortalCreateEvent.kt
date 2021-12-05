package de.chaosolymp.portals.bungee.event

import net.md_5.bungee.api.plugin.Cancellable
import net.md_5.bungee.api.plugin.Event
import java.util.*

class PortalCreateEvent(val owner: UUID, val name: String, val server: String, val public: Boolean, val world: String, val x: Int, val y: Int, val z: Int) : Event(), Cancellable {
    private var cancel: Boolean = false

    override fun setCancelled(cancel: Boolean) {
        this.cancel = cancel
    }

    override fun isCancelled() = cancel
}