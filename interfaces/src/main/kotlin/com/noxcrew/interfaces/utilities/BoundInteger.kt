package com.noxcrew.interfaces.utilities

import com.noxcrew.interfaces.properties.DelegateTrigger
import com.noxcrew.interfaces.properties.Trigger
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/** A changeable integer that cannot go below [min] or above [max]. */
public class BoundInteger(
    initial: Int,
    public var min: Int,
    public var max: Int,
) : ObservableProperty<Int>(initial), Trigger {

    private val delegateTrigger = DelegateTrigger()

    override fun beforeChange(property: KProperty<*>, oldValue: Int, newValue: Int): Boolean {
        val acceptableRange = min..max

        if (newValue in acceptableRange) {
            return true
        }

        val coercedValue = newValue.coerceIn(acceptableRange)
        var value by this

        value = coercedValue

        return false
    }

    override fun afterChange(property: KProperty<*>, oldValue: Int, newValue: Int) {
        if (oldValue != newValue) {
            trigger()
        }
    }

    override fun trigger() {
        delegateTrigger.trigger()
    }

    override fun <T : Any> addListener(reference: T, listener: T.() -> Unit) {
        delegateTrigger.addListener(reference, listener)
    }

    /** Returns whether there is some value above the current value that is not above max. */
    public fun hasSucceeding(): Boolean {
        val value by this
        return value < max
    }

    /** Returns whether there is some value below the current value that is not below min. */
    public fun hasPreceeding(): Boolean {
        val value by this
        return value > min
    }
}
