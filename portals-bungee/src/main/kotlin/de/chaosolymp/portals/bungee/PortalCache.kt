package de.chaosolymp.portals.bungee

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import de.chaosolymp.portals.bungee.config.CacheConfigurationElement

internal class PortalCache(plugin: BungeePlugin) {
    val idNameCache: Cache<Int, String> = createCache(plugin.cachingConfiguration.idNameCache, "Id -> Name mapping")

    val nameIdCache: Cache<String, Int> = createCache(plugin.cachingConfiguration.nameIdCache, "Name -> Id mapping")

    val idPublicCache: Cache<Int, Boolean> = createCache(plugin.cachingConfiguration.idPublicCache, "Id -> Public mapping")

    val namePublicCache: Cache<String, Boolean> = createCache(plugin.cachingConfiguration.namePublicCache, "Name -> Public mapping")

    val linkCache: Cache<Int, Int> = createCache(plugin.cachingConfiguration.linkCache, "Link source -> Link destination mapping")

    private fun <K, V> createCache(cache: CacheConfigurationElement, debugName: String): Cache<K, V> {
        val underlyingCache = Caffeine.newBuilder().apply {
            if (cache.initialCapacity != null) initialCapacity(cache.initialCapacity)
            if (cache.maximumSize != null) maximumSize(cache.maximumSize)
            if (cache.maximumWeight != null) maximumWeight(cache.maximumWeight)
            if (cache.expireAfterAccess != null) expireAfterAccess(
                cache.expireAfterAccess.first,
                cache.expireAfterAccess.second
            )
            if (cache.expireAfterWrite != null) expireAfterWrite(
                cache.expireAfterWrite.first,
                cache.expireAfterWrite.second
            )

        }.build<K, V>()

        return DebugCacheWrapper(underlyingCache, debugName)
    }
}