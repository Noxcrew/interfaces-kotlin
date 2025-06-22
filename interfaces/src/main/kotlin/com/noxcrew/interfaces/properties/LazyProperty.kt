package com.noxcrew.interfaces.properties

/** A property that is lazily evaluated. */
public abstract class LazyProperty<T : Any> : DelegateTrigger() {
    /** A simple lazy property backed by [block]. */
    public class Simple<T : Any>(private val block: suspend (Boolean) -> T) : LazyProperty<T>() {
        override suspend fun load(reload: Boolean): T = block(reload)
    }

    /** The current value of this property. */
    protected var value: T? = null

    /** Resets this value, requiring it to be reloaded. */
    public fun reset() {
        value = null
    }

    /** Loads the value of this property. */
    public abstract suspend fun load(reload: Boolean = true): T

    /**
     * Triggers a re-evaluation of the property.
     * If [reload] is given, all data should be fully reloaded.
     */
    public suspend fun reevaluate(trigger: Boolean = true, reload: Boolean = true): T = load(reload).also {
        value = it
        if (trigger) trigger()
    }

    /** Returns the value of this property. */
    public suspend fun getValue(): T = value ?: reevaluate(false)

    /** Returns the cached value of this property. */
    public fun getCachedValue(): T? = value
}
