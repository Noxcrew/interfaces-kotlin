package com.noxcrew.interfaces.utilities

import kotlin.reflect.KProperty

/** An integer that is incremented after each read. */
internal class IncrementingInteger {
    private var value: Int = 0
        get() = field++

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Int = value
}
