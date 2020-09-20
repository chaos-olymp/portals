package de.chaosolymp.portals.bungee.event

import de.chaosolymp.portals.core.Portal
import net.md_5.bungee.api.plugin.Cancellable
import net.md_5.bungee.api.plugin.Event

class PortalRemoveEvent(val portal: Portal): Event(), Cancellable {
    private var cancel: Boolean = false

    override fun setCancelled(cancel: Boolean) {
        this.cancel = cancel
    }

    override fun isCancelled() = this.cancel
}