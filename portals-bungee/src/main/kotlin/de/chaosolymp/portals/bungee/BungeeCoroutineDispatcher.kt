package de.chaosolymp.portals.bungee

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

class BungeeCoroutineDispatcher(private val plugin: BungeePlugin) : CoroutineDispatcher() {
    /**
     * Dispatches execution of a runnable [block] onto another thread in the given [context].
     * This method should guarantee that the given [block] will be eventually invoked,
     * otherwise the system may reach a deadlock state and never leave it.
     * Cancellation mechanism is transparent for [CoroutineDispatcher] and is managed by [block] internals.
     *
     * This method should generally be exception-safe. An exception thrown from this method
     * may leave the coroutines that use this dispatcher in the inconsistent and hard to debug state.
     *
     * This method must not immediately call [block]. Doing so would result in [StackOverflowError]
     * when [yield] is repeatedly called from a loop. However, an implementation that returns `false` from
     * [isDispatchNeeded] can delegate this function to `dispatch` method of [Dispatchers.Unconfined], which is
     * integrated with [yield] to avoid this problem.
     */
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        plugin.proxy.scheduler.runAsync(plugin, block)
    }
}