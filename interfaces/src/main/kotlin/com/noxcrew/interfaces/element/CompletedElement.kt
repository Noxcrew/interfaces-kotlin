package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.click.ClickHandler
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** A drawn element that includes an [itemStack] to show and a [clickHandler] to run. */
public data class CompletedElement(
    public var itemStack: ItemStack?,
    public val clickHandler: ClickHandler,
    public var pendingLazy: LazyElement? = null,
)

/** Completes drawing this element for [player]. */
public suspend fun Element.complete(player: Player): CompletedElement = CompletedElement(
    drawable().draw(player),
    clickHandler(),
    this as? LazyElement
)
