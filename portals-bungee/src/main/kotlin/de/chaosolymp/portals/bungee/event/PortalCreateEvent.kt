package de.chaosolymp.portals.bungee.event

import net.md_5.bungee.api.plugin.Cancellable
import net.md_5.bungee.api.plugin.Event

class PortalCreateEvent(): Event(), Cancellable {
    private var cancel: Boolean = false

    override fun setCancelled(cancel: Boolean) {
        this.cancel = cancel
    }

    override fun isCancelled() = this.cancel
}