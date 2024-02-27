package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.click.ClickHandler
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

internal data class CompletedElement(
    public val itemStack: ItemStack?,
    public val clickHandler: ClickHandler
)

internal suspend fun Element.complete(player: Player) = CompletedElement(
    drawable().draw(player),
    clickHandler()
)
