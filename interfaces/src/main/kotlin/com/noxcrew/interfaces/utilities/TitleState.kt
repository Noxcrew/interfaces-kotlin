package com.noxcrew.interfaces.utilities

import net.kyori.adventure.text.Component

/** Manages the state of a title. */
public class TitleState(initialState: Component?) {

    /** The current value of the title. */
    public var current: Component? = initialState
        set(value) {
            // Don't update if nothing changed
            if (field == value) return

            hasChanged = true
            field = value
        }
        get() {
            hasChanged = false
            return field
        }

    /** Whether the title has changed at some point. */
    public var hasChanged: Boolean = false
}
