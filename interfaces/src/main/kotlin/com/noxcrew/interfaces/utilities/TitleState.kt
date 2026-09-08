package com.noxcrew.interfaces.utilities

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** Manages the state of a title. */
public class TitleState(
    initialState: Component? = null,
    initialSupplier: (suspend (Player) -> Component?)? = null,
) {

    /** A supplier for a new title. */
    public var supplier: (suspend (Player) -> Component?)? = initialSupplier

    /** The current value of the title. */
    public var current: Component? = initialState
        set(value) {
            // Don't update if nothing changed
            if (field == value) return
            dirty = true
            field = value
        }

    /** Whether the title should be updated from its supplier. */
    public var dirty: Boolean = false
        private set

    /** Refreshes the title, re-running [supplier]. */
    public fun markDirty() {
        dirty = true
    }

    /** Cleans up this title state. */
    public fun clean() {
        dirty = false
    }
}
