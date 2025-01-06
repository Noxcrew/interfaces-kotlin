package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.click.ClickHandler
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** A drawn element that includes an [itemStack] to show and a [clickHandler] to run. */
public data class CompletedElement(
    public val itemStack: ItemStack?,
    public val clickHandler: ClickHandler,
)

/** Completes drawing this element for [player]. */
public suspend fun Element.complete(player: Player): CompletedElement = CompletedElement(
    drawable().draw(player),
    clickHandler(),
)
