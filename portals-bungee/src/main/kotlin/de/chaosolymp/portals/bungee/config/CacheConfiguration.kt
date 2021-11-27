package de.chaosolymp.portals.bungee.config

import java.util.concurrent.TimeUnit

data class CacheConfiguration(val idNameCache: CacheConfigurationElement, val nameIdCache: CacheConfigurationElement, val idPublicCache: CacheConfigurationElement, val namePublicCache: CacheConfigurationElement, val linkCache: CacheConfigurationElement) {
    companion object {
        fun getDefaultConfiguration(): CacheConfiguration {
            val configurationElement = CacheConfigurationElement(null, 128, null, null, null)
            return CacheConfiguration(
                configurationElement,
                configurationElement,
                configurationElement,
                configurationElement,
                configurationElement
            )
        }
    }
}


data class CacheConfigurationElement(val initialCapacity: Int?, val maximumSize: Long?, val maximumWeight: Long?, val expireAfterAccess: Pair<Long, TimeUnit>?, val expireAfterWrite: Pair<Long, TimeUnit>?)