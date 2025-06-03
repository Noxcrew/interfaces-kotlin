package com.noxcrew.interfaces.properties

import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/** An observable property that is triggered when its value changes. */
public open class InterfaceProperty<T>(
    defaultValue: T
) : ObservableProperty<T>(defaultValue), Trigger by DelegateTrigger() {

    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
        if (oldValue != newValue) {
            trigger()
        }
    }
}
