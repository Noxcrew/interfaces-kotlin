package com.noxcrew.interfaces.utilities

import net.kyori.adventure.text.Component

/** Manages the state of a title. */
internal class TitleState(initialState: Component?) {

    /** The current value of the title. */
    internal var current = initialState
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
    internal var hasChanged = false
}
