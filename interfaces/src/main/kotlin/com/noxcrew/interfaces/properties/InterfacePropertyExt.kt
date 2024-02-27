package com.noxcrew.interfaces.properties

/** Creates a new interfaces property out of [value]. */
public fun <T> interfaceProperty(value: T): InterfaceProperty<T> = InterfaceProperty(value)

/** Creates an empty trigger. */
public fun emptyTrigger(): Trigger = EmptyTrigger
