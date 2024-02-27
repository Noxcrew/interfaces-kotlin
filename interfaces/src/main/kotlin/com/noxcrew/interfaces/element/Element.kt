package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.drawable.Drawable
import org.bukkit.Material

public interface Element {

    public companion object EMPTY : Element by StaticElement(Drawable.drawable(Material.AIR))

    public fun drawable(): Drawable

    public fun clickHandler(): ClickHandler
}
