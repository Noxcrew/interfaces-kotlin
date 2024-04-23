package com.noxcrew.interfaces.properties

import com.github.benmanes.caffeine.cache.Caffeine

/** A trigger that delegates triggers to its listeners. */
public open class DelegateTrigger : Trigger {

    private val updateListeners = Caffeine.newBuilder()
        .weakKeys()
        .build<Any, MutableList<Any.() -> Unit>>()
        .asMap()

    override fun trigger() {
        updateListeners.forEach { (_, listeners) ->
            listeners.forEach { it() }
        }
    }

    override fun <T : Any> addListener(reference: T, listener: T.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        updateListeners.computeIfAbsent(reference) {
            mutableListOf()
        }.add(listener as (Any.() -> Unit))
    }
}
