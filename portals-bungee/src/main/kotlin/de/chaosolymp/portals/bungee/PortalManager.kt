package de.chaosolymp.portals.bungee

import de.chaosolymp.portals.bungee.event.PortalCreateEvent
import de.chaosolymp.portals.bungee.event.PortalRemoveEvent
import de.chaosolymp.portals.core.DatabaseService
import de.chaosolymp.portals.core.NumberUtils
import de.chaosolymp.portals.core.Portal
import de.chaosolymp.portals.core.PortalListType
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.time.Instant
import java.util.*

class PortalManager(private val plugin: BungeePlugin, private val databaseService: DatabaseService) {

    private val regex = Regex("^[a-zA-Z0-9_-]+")

    fun createTable() = databaseService.createTable()

    fun isNameValid(name: String) = name.isNotEmpty() && name.length < 33 && !NumberUtils.isNumber(name) && regex.matches(name)

    fun doesNameExist(name: String): Boolean = databaseService.doesNameExist(name.lowercase(Locale.getDefault()))
    
    fun createPortal(owner: UUID, name: String, server: String, public: Boolean, world: String, x: Int, y: Int, z: Int, yaw: Float, pitch: Float): Int? {
        // Call event for external listeners
        val event = PortalCreateEvent(owner, name.lowercase(Locale.getDefault()), server, public, world, x, y, z)
        plugin.proxy.pluginManager.callEvent(event)

        if(event.isCancelled) {
            plugin.logger.info("Portal creation cancelled")
            return null
        }

        // Store on database
        val result = databaseService.createPortal(owner, name.lowercase(Locale.getDefault()), server, public, world, x, y, z, yaw, pitch)
        if(result == null) {
            plugin.logger.severe("Database error occurred in createPortal (Generated Key not present!)")
        } else {
            plugin.logger.info("Created portal with id #$result")
        }

        return result
    }

    fun getDbTime(): Instant = databaseService.getDbTime()

    fun setPublic(id: Int, public: Boolean) {
        // Store on database
        databaseService.setPublic(id, public)

        // Cache for faster access times
        plugin.portalCache.idPublicCache.put(id, public)

        plugin.logger.info("Set portal with id #$id ${if(public) "public" else "private"}")
    }

    fun setPublic(name: String, public: Boolean) {
        // Store on database
        databaseService.setPublic(name, public)

        // Cache for faster access times
        plugin.portalCache.namePublicCache.put(name, public)

        plugin.logger.info("Set portal with name `$name` ${if(public) "public" else "private"}")
    }

    fun getPortals(offset: Int, limit: Int): Collection<Portal> = databaseService.getPortals(offset, limit)

    fun getPortal(id: Int): Portal? = databaseService.getPortal(id)

    fun getPortal(name: String): Portal? = databaseService.getPortal(name.lowercase(Locale.getDefault()))

    fun remove(id: Int) {
        // Call event for external listeners
        val event = PortalRemoveEvent()
        plugin.proxy.pluginManager.callEvent(event)

        if(event.isCancelled) {
            return
        }

        // Invalidate cache values
        plugin.portalCache.idPublicCache.invalidate(id)
        plugin.portalCache.idNameCache.invalidate(id)
        plugin.portalCache.linkCache.invalidateAll()
        plugin.portalCache.nameIdCache.invalidateAll()

        // Delete from database
        databaseService.remove(id)
        plugin.logger.info("Removed portal with id #$id")
    }

    fun remove(name: String) {
        // Call event for external listeners
        val event = PortalRemoveEvent()
        plugin.proxy.pluginManager.callEvent(event)

        if(event.isCancelled) {
            return
        }

        // Invalidate cache values
        plugin.portalCache.namePublicCache.invalidate(name)
        plugin.portalCache.nameIdCache.invalidate(name)
        plugin.portalCache.linkCache.invalidateAll()
        plugin.portalCache.idNameCache.invalidateAll()

        // Delete from database
        databaseService.remove(name)
        plugin.logger.info("Removed portal with name `$name`")
    }

    fun getIdOfName(name: String): Int {
        val lowerName = name.lowercase(Locale.getDefault())

        val cacheValue = plugin.portalCache.nameIdCache.getIfPresent(lowerName)
        if(cacheValue != null) return cacheValue

        val id = databaseService.getIdOfName(lowerName)

        // Cache for faster access times
        plugin.portalCache.nameIdCache.put(lowerName, id)
        plugin.portalCache.idNameCache.put(id, lowerName)

        return id
    }

    fun link(originId: Int, linkId: Int) {
        // Store on database
        databaseService.link(originId, linkId)

        // Cache for faster access times
        plugin.portalCache.linkCache.put(originId, linkId)

        plugin.logger.info("Linked portal with id #$originId with #$linkId")
    }

    fun rename(id: Int, name: String) {
        val lowerName = name.lowercase(Locale.getDefault())
        databaseService.rename(id, lowerName)

        // Invalidate cache
        plugin.portalCache.idNameCache.invalidate(id)
        plugin.portalCache.nameIdCache.invalidateAll()

        // Cache for faster access times
        plugin.portalCache.nameIdCache.put(lowerName, id)
        plugin.portalCache.idNameCache.put(id, lowerName)

        plugin.logger.info("Renamed portal with id #$id to `$lowerName`")
    }

    fun setDisplayName(id: Int, name: String) {
        databaseService.setDisplayName(id, name)
        plugin.logger.info("Set display name of portal with id #$id to `$name`")
    }

    fun isPublic(id: Int): Boolean {
        val cacheValue = plugin.portalCache.idPublicCache.getIfPresent(id)
        if(cacheValue != null) {
            return cacheValue
        }

        val result = databaseService.isPublic(id)

        // Cache for faster access times
        plugin.portalCache.idPublicCache.put(id, result)

        return result
    }

    fun doesPlayerOwnPortal(player: UUID, id: Int): Boolean = databaseService.doesPlayerOwnPortal(player, id)

    fun getNameOfId(id: Int): String {
        val cachedValue = plugin.portalCache.idNameCache.getIfPresent(id)
        if(cachedValue != null) return cachedValue

        val name = databaseService.getNameOfId(id)

        // Cache for faster access times
        plugin.portalCache.idNameCache.put(id, name)

        return name
    }

    fun countPortals(): Int = databaseService.countPortals()

    fun countPublicPortals(): Int = databaseService.countPublicPortals()

    fun countPortalsOfPlayer(uuid: UUID): Int = databaseService.countPortalsOfPlayer(uuid)

    fun doesNameOrIdExist(nameOrId: String): Boolean = databaseService.doesNameOrIdExist(nameOrId)

    fun doesPlayerOwnPortalOrHasOtherAccess(sender: CommandSender, id: Int): Boolean = if(sender is ProxiedPlayer) {
        databaseService.doesPlayerOwnPortal(sender.uniqueId, id)
    } else {
        false
    }

    fun doesIdExists(id: Int): Boolean = databaseService.doesIdExists(id)

    fun getPortals(sender: CommandSender, listType: PortalListType, skip: Int, count: Int): Iterable<Portal> = databaseService.getPortals(if(sender is ProxiedPlayer) sender.uniqueId else null, listType, skip, count)

    fun getPortalIdAt(server: String, world: String, x: Int, y: Int, z: Int): Int? = databaseService.getPortalIdAt(server, world, x, y, z)

    fun getPortalLink(id: Int): Int? {
        val cachedTargetId = plugin.portalCache.linkCache.getIfPresent(id)
        if(cachedTargetId != null) return cachedTargetId

        val portalLink = databaseService.getPortalLink(id)

        // Cache for faster access times
        if(portalLink != null) plugin.portalCache.linkCache.put(id, portalLink)

        return portalLink
    }
}