package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.drawable.Drawable
import org.bukkit.Material

/** An element in an interface that is drawn by [drawable] and triggers [clickHandler]. */
public interface Element {

    /** An empty element that shows as air and has no function. */
    public companion object EMPTY : Element by StaticElement(Drawable.drawable(Material.AIR))

    /** Whether the slot of this element can be modified. */
    public val isSlotModifiable: Boolean
        get() = false

    /** Returns the drawable for this element. */
    public fun drawable(): Drawable

    /** Returns the click handler for this element. */
    public fun clickHandler(): ClickHandler
}
