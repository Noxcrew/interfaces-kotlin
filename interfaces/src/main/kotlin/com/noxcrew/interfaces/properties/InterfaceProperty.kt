package com.noxcrew.interfaces.properties

import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/** An observable property that is triggered when its value changes. */
public open class InterfaceProperty<T>(
    defaultValue: T
) : ObservableProperty<T>(defaultValue), Trigger by DelegateTrigger() {

    /** Modifies the value of this property. */
    public var value: T
        get() = getValue(null, ::value)
        set(value) = setValue(null, ::value, value)

    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
        if (oldValue != newValue) {
            trigger()
        }
    }
}
