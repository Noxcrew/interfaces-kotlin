package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** A drawn element that includes an [itemStack] to show and a [clickHandler] to run. */
public data class CompletedElement(
    public var itemStack: ItemStack?,
    public val clickHandler: ClickHandler? = null,
    public var pendingLazy: LazyElement? = null,
    public val isSlotModifiable: Boolean,
)

/** Completes drawing this element for [player]. */
public suspend fun Element.complete(player: Player, view: InterfaceView): CompletedElement {
    val lazy = this as? LazyElement
    val completed = CompletedElement(
        drawable().draw(player).takeUnless { it.isEmpty },
        clickHandler(),
        lazy,
        isSlotModifiable,
    )

    // Whenever any of the re-decoration triggers go off we re-add the lazy field!
    // We tie the listener to the completed element so they get garbage collected
    // automatically when the completed element is destroyed.
    lazy?.getRedecorationTriggers(player)?.forEach {
        it.addListener(completed) {
            completed.pendingLazy = lazy
            view.ensureDecorating(completed)
        }
    }
    return completed
}
