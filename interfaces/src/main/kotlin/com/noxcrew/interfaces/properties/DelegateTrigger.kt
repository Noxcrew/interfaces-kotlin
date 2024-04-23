package com.noxcrew.interfaces.properties

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedDeque

/** A trigger that delegates triggers to its listeners. */
public open class DelegateTrigger : Trigger {

    private val updateListeners = Caffeine.newBuilder()
        .weakKeys()
        .build<Any, Queue<Any.() -> Unit>>()
        .asMap()

    override fun trigger() {
        updateListeners.forEach { (obj, listeners) ->
            listeners.forEach { obj?.apply(it) }
        }
    }

    override fun <T : Any> addListener(reference: T, listener: T.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        updateListeners.computeIfAbsent(reference) {
            ConcurrentLinkedDeque()
        }.add(listener as (Any.() -> Unit))
    }
}
