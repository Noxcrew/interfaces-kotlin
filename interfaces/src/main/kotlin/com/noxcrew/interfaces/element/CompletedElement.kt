package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.click.ClickHandler
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** A drawn element that includes an [itemStack] to show and a [clickHandler] to run. */
internal data class CompletedElement(
    val itemStack: ItemStack?,
    val clickHandler: ClickHandler
)

/** Completes drawing this element for [player]. */
internal suspend fun Element.complete(player: Player) = CompletedElement(
    drawable().draw(player),
    clickHandler()
)
