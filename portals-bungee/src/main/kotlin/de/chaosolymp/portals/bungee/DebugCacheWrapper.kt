package de.chaosolymp.portals.bungee

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Policy
import com.github.benmanes.caffeine.cache.stats.CacheStats
import java.util.concurrent.ConcurrentMap
import java.util.function.Function

class DebugCacheWrapper<K, V>(private val underlyingCache: Cache<K, V>, private val debugName: String) : Cache<K, V> {


    /**
     * Returns the value associated with the `key` in this cache, or `null` if there is no
     * cached value for the `key`.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or `null` if this cache contains
     * no mapping for the key
     * @throws NullPointerException if the specified key is null
     */
    override fun getIfPresent(key: K): V? {
        val value = underlyingCache.getIfPresent(key)
        DebugMessenger.verbose("Cache [$debugName]", "Cache access Cache#getIfPresent (key = $key, value = $value)")
        return value
    }

    /**
     * Returns the value associated with the `key` in this cache, obtaining that value from the
     * `mappingFunction` if necessary. This method provides a simple substitute for the
     * conventional "if cached, return; otherwise create, cache and return" pattern.
     *
     *
     * If the specified key is not already associated with a value, attempts to compute its value
     * using the given mapping function and enters it into this cache unless `null`. The entire
     * method invocation is performed atomically, so the function is applied at most once per key.
     * Some attempted update operations on this cache by other threads may be blocked while the
     * computation is in progress, so the computation should be short and simple, and must not attempt
     * to update any other mappings of this cache.
     *
     *
     * **Warning:** as with [CacheLoader.load], `mappingFunction` **must not**
     * attempt to update any other mappings of this cache.
     *
     * @param key the key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with the specified key, or null if
     * the computed value is null
     * @throws NullPointerException if the specified key or mappingFunction is null
     * @throws IllegalStateException if the computation detectably attempts a recursive update to this
     * cache that would otherwise never complete
     * @throws RuntimeException or Error if the mappingFunction does so, in which case the mapping is
     * left unestablished
     */
    override fun get(key: K, mappingFunction: Function<in K, out V>?): V {
        val value = underlyingCache.get(key, mappingFunction)
        DebugMessenger.verbose("Cache [$debugName]", "Cache access Cache#get (key = $key, value = $value)")
        return value;
    }

    /**
     * Returns a map of the values associated with the `keys` in this cache. The returned map
     * will only contain entries which are already present in the cache.
     *
     *
     * Note that duplicate elements in `keys`, as determined by [Object.equals], will be
     * ignored.
     *
     * @param keys the keys whose associated values are to be returned
     * @return the unmodifiable mapping of keys to values for the specified keys found in this cache
     * @throws NullPointerException if the specified collection is null or contains a null element
     */
    override fun getAllPresent(keys: MutableIterable<K>?): MutableMap<K, V> = underlyingCache.getAllPresent(keys)

    /**
     * Returns a map of the values associated with the `keys`, creating or retrieving those
     * values if necessary. The returned map contains entries that were already cached, combined with
     * the newly loaded entries; it will never contain null keys or values.
     *
     *
     * A single request to the `mappingFunction` is performed for all keys which are not already
     * present in the cache. All entries returned by `mappingFunction` will be stored in the
     * cache, over-writing any previously cached values. If another call to [.get] tries to load
     * the value for a key in `keys`, implementations may either have that thread load the entry
     * or simply wait for this thread to finish and return the loaded value. In the case of
     * overlapping non-blocking loads, the last load to complete will replace the existing entry. Note
     * that multiple threads can concurrently load values for distinct keys.
     *
     *
     * Note that duplicate elements in `keys`, as determined by [Object.equals], will be
     * ignored.
     *
     * @param keys the keys whose associated values are to be returned
     * @param mappingFunction the function to compute the values
     * @return an unmodifiable mapping of keys to values for the specified keys in this cache
     * @throws NullPointerException if the specified collection is null or contains a null element, or
     * if the map returned by the mappingFunction is null
     * @throws RuntimeException or Error if the mappingFunction does so, in which case the mapping is
     * left unestablished
     */
    override fun getAll(
        keys: MutableIterable<K>?,
        mappingFunction: Function<in MutableSet<out K>, out MutableMap<out K, out V>>?
    ): MutableMap<K, V> = underlyingCache.getAll(keys, mappingFunction)

    /**
     * Associates the `value` with the `key` in this cache. If the cache previously
     * contained a value associated with the `key`, the old value is replaced by the new
     * `value`.
     *
     *
     * Prefer [.get] when using the conventional "if cached, return; otherwise
     * create, cache and return" pattern.
     *
     * @param key the key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @throws NullPointerException if the specified key or value is null
     */
    override fun put(key: K, value: V) {
        DebugMessenger.verbose("Cache [$debugName]", "Cache#put (key = $key, value = $value)")
        underlyingCache.put(key, value)
    }

    /**
     * Copies all of the mappings from the specified map to the cache. The effect of this call is
     * equivalent to that of calling `put(k, v)` on this map once for each mapping from key
     * `k` to value `v` in the specified map. The behavior of this operation is undefined
     * if the specified map is modified while the operation is in progress.
     *
     * @param map the mappings to be stored in this cache
     * @throws NullPointerException if the specified map is null or the specified map contains null
     * keys or values
     */
    override fun putAll(map: MutableMap<out K, out V>?) = underlyingCache.putAll(map)

    /**
     * Discards any cached value for the `key`. The behavior of this operation is undefined for
     * an entry that is being loaded (or reloaded) and is otherwise not present.
     *
     * @param key the key whose mapping is to be removed from the cache
     * @throws NullPointerException if the specified key is null
     */
    override fun invalidate(key: K) {
        DebugMessenger.verbose("Cache [$debugName]", "Cache invalidation - Cache#invalidate (key = $key)")
        underlyingCache.invalidate(key)
    }

    /**
     * Discards any cached values for the `keys`. The behavior of this operation is undefined
     * for an entry that is being loaded (or reloaded) and is otherwise not present.
     *
     * @param keys the keys whose associated values are to be removed
     * @throws NullPointerException if the specified collection is null or contains a null element
     */
    override fun invalidateAll(keys: MutableIterable<K>?) = underlyingCache.invalidateAll(keys)

    /**
     * Discards all entries in the cache. The behavior of this operation is undefined for an entry
     * that is being loaded (or reloaded) and is otherwise not present.
     */
    override fun invalidateAll() {
        DebugMessenger.verbose("Cache [$debugName]", "Cache invalidation - Cache#invalidate (key = *)")
        underlyingCache.invalidateAll()
    }

    /**
     * Returns the approximate number of entries in this cache. The value returned is an estimate; the
     * actual count may differ if there are concurrent insertions or removals, or if some entries are
     * pending removal due to expiration or weak/soft reference collection. In the case of stale
     * entries this inaccuracy can be mitigated by performing a [.cleanUp] first.
     *
     * @return the estimated number of mappings
     */
    override fun estimatedSize(): Long = underlyingCache.estimatedSize()

    /**
     * Returns a current snapshot of this cache's cumulative statistics. All statistics are
     * initialized to zero, and are monotonically increasing over the lifetime of the cache.
     *
     *
     * Due to the performance penalty of maintaining statistics, some implementations may not record
     * the usage history immediately or at all.
     *
     * @return the current snapshot of the statistics of this cache
     */
    override fun stats(): CacheStats = underlyingCache.stats()

    /**
     * Returns a view of the entries stored in this cache as a thread-safe map. Modifications made to
     * the map directly affect the cache.
     *
     *
     * A computation operation, such as [ConcurrentMap.compute], performs the entire method
     * invocation atomically, so the function is applied at most once per key. Some attempted update
     * operations by other threads may be blocked while computation is in progress. The computation
     * must not attempt to update any other mappings of this cache.
     *
     *
     * Iterators from the returned map are at least *weakly consistent*: they are safe for
     * concurrent use, but if the cache is modified (including by eviction) after the iterator is
     * created, it is undefined which of the changes (if any) will be reflected in that iterator.
     *
     * @return a thread-safe view of this cache supporting all of the optional [Map] operations
     */
    override fun asMap(): ConcurrentMap<K, V> = underlyingCache.asMap()

    /**
     * Performs any pending maintenance operations needed by the cache. Exactly which activities are
     * performed -- if any -- is implementation-dependent.
     */
    override fun cleanUp() {
        underlyingCache.cleanUp()
    }

    /**
     * Returns access to inspect and perform low-level operations on this cache based on its runtime
     * characteristics. These operations are optional and dependent on how the cache was constructed
     * and what abilities the implementation exposes.
     *
     *
     * **Warning:** policy operations **must not** be performed from within an atomic scope of
     * another cache operation.
     *
     * @return access to inspect and perform advanced operations based on the cache's characteristics
     */
    override fun policy(): Policy<K, V> = underlyingCache.policy()
}