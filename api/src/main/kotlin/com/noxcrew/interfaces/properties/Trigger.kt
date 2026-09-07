package com.noxcrew.interfaces.properties

/** A generic object that can be triggered which will invoke its listeners. */
public interface Trigger {

    /** Triggers the listeners to be invoked. */
    public fun trigger()

    /**
     * Adds a new listener to this trigger. Garbage collection of this listener is
     * tied to the lifetime of [reference]. The [listener] itself should only ever
     * reference the passed instance of [reference] and not [reference] directly
     * to avoid situations where the existence of [listener] holds the [reference]
     * captive, preventing it from being garbage collected.
     */
    public fun <T : Any> addListener(reference: T, listener: T.() -> Unit)
}
