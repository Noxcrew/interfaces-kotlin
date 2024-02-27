package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.drawable.Drawable

public class StaticElement public constructor(
    private val drawable: Drawable,
    private val clickHandler: ClickHandler = ClickHandler.EMPTY
) : Element {

    override fun drawable(): Drawable = drawable

    override fun clickHandler(): ClickHandler = clickHandler
}
