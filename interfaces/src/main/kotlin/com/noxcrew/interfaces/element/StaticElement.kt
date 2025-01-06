package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.drawable.Drawable

/** A static element that uses [drawable] and a set [clickHandler]. */
public class StaticElement(
    private val drawable: Drawable,
    private val clickHandler: ClickHandler = ClickHandler.EMPTY,
) : Element {

    override fun drawable(): Drawable = drawable

    override fun clickHandler(): ClickHandler = clickHandler
}
