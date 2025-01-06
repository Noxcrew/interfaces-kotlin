package com.noxcrew.interfaces.transform.builtin

import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.Trigger
import com.noxcrew.interfaces.transform.ReactiveTransform
import com.noxcrew.interfaces.utilities.BoundInteger
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/** A transform that adds multiple pages which can be clicked through using pagination buttons. */
public abstract class PagedTransformation<P : Pane>(
    private val back: PaginationButton,
    private val forward: PaginationButton,
    extraTriggers: Array<Trigger> = emptyArray(),
) : ReactiveTransform<P> {

    /** The current page of this transform, bound between 0 and the integer limit. */
    protected val boundPage: BoundInteger = BoundInteger(0, 0, Integer.MAX_VALUE)

    /** The current page of the transform. */
    protected var page: Int by boundPage

    override suspend fun invoke(pane: P, view: InterfaceView) {
        if (boundPage.hasPreceeding()) {
            applyButton(pane, back)
        }

        if (boundPage.hasSucceeding()) {
            applyButton(pane, forward)
        }
    }

    /** Places the given [button] in [pane]. */
    protected open fun applyButton(pane: Pane, button: PaginationButton) {
        val (point, drawable, increments) = button

        pane[point] = StaticElement(drawable) { (player, _, click) ->
            increments[click]?.let { increment -> page += increment }
            button.clickHandler(player)
        }
    }

    override val triggers: Array<Trigger> = arrayOf<Trigger>(boundPage).plus(extraTriggers)
}

/** A button used by a [PagedTransformation]. */
public data class PaginationButton(
    /** The position of this button. */
    public val position: GridPoint,
    /** The drawable to use for this button. */
    public val drawable: Drawable,
    /** The increments to apply to the current page number based on the incoming click type. */
    public val increments: Map<ClickType, Int>,
    /** An optional additional click handler to run when this button is used. */
    public val clickHandler: (Player) -> Unit = {},
)
