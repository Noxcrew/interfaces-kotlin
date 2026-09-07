package com.noxcrew.interfaces.utilities

import com.noxcrew.interfaces.properties.InterfaceProperty
import kotlin.reflect.KProperty

/** A changeable integer that cannot go below [min] or above [max]. */
public class BoundInteger(
    initial: Int,
    public var min: Int,
    public var max: Int,
) : InterfaceProperty<Int>(initial) {

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

    /** Returns whether there is some value above the current value that is not above max. */
    public fun hasSucceeding(): Boolean {
        val value by this
        return value < max
    }

    /** Returns whether there is some value below the current value that is not below min. */
    public fun hasPreceding(): Boolean {
        val value by this
        return value > min
    }
}
