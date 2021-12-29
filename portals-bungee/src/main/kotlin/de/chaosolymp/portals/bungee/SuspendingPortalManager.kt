package de.chaosolymp.portals.bungee

import de.chaosolymp.portals.core.Portal
import de.chaosolymp.portals.core.PortalListType
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.CommandSender
import java.time.Instant
import java.util.*

class SuspendingPortalManager(private val plugin: BungeePlugin) {
    suspend fun doesNameExist(name: String): Boolean = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.doesNameExist(name)
    }

    suspend fun createPortal(owner: UUID, name: String, server: String, public: Boolean, world: String, x: Int, y: Int, z: Int, yaw: Float, pitch: Float): Int? = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.createPortal(owner, name, server, public, world, x, y, z, yaw, pitch)
    }

    suspend fun getDbTime(): Instant = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.getDbTime()
    }

    suspend fun setPublic(id: Int, public: Boolean) = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.setPublic(id, public)
    }

    suspend fun setPublic(name: String, public: Boolean) = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.setPublic(name, public)
    }

    suspend fun getPortals(offset: Int, limit: Int): Collection<Portal> = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.getPortals(offset, limit)
    }

    suspend fun getPortal(id: Int): Portal? = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.getPortal(id)
    }

    suspend fun getPortal(name: String): Portal? = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.getPortal(name)
    }

    suspend fun remove(id: Int) = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.remove(id)
    }

    suspend fun remove(name: String) = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.remove(name)
    }

    suspend fun getIdOfName(name: String): Int = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.getIdOfName(name)
    }

    suspend fun getNameOfId(id: Int): String = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.getNameOfId(id)
    }

    suspend fun link(originId: Int, linkId: Int) = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.link(originId, linkId)
    }

    suspend fun rename(id: Int, name: String) = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.rename(id, name)
    }

    suspend fun setDisplayName(id: Int, name: String) = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.setDisplayName(id, name)
    }

    suspend fun isPublic(id: Int): Boolean = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.isPublic(id)
    }

    suspend fun doesPlayerOwnPortal(player: UUID, id: Int): Boolean = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.doesPlayerOwnPortal(player, id)
    }

    suspend fun doesPlayerOwnPortalOrHasOtherAccess(sender: CommandSender, id: Int) = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.doesPlayerOwnPortalOrHasOtherAccess(sender, id)
    }

    suspend fun doesNameOrIdExist(nameOrId: String): Boolean = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.doesNameOrIdExist(nameOrId)
    }

    suspend fun getPortals(sender: CommandSender, listType: PortalListType, skip: Int, count: Int): Iterable<Portal> = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.getPortals(sender, listType, skip, count)
    }

    suspend fun getPortalIdAt(server: String, world: String, x: Int, y: Int, z: Int): Int? = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.getPortalIdAt(server, world, x, y, z)
    }

    suspend fun getPortalLink(id: Int): Int? = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.getPortalLink(id)
    }

    suspend fun doesIdExists(id: Int): Boolean = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.doesIdExists(id)
    }

    suspend fun countPortals(): Int = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.countPortals()
    }

    suspend fun countPublicPortals(): Int = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.countPublicPortals()
    }

    suspend fun countPortalsOfPlayer(uuid: UUID): Int = withContext(plugin.coroutineDispatcher) {
        return@withContext plugin.portalManager.countPortalsOfPlayer(uuid)
    }
}