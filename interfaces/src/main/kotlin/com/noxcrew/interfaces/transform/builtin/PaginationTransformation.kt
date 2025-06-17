package com.noxcrew.interfaces.transform.builtin

import com.noxcrew.interfaces.element.Element
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.grid.GridPositionGenerator
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.view.InterfaceView
import kotlin.properties.Delegates

/** A [PagedTransformation] where the positions of the elements are determined by [positionGenerator]. */
public abstract class PaginationTransformation<P : Pane, E>(
    private val positionGenerator: GridPositionGenerator,
    default: Collection<E>,
    back: PaginationButton? = null,
    forward: PaginationButton? = null,
) : PagedTransformation<P>(back, forward) {
    /** A simple pagination transformation where the elements in the list are interfaces elements. */
    public open class Simple<P : Pane>(
        positionGenerator: GridPositionGenerator,
        default: Collection<Element>,
        back: PaginationButton? = null,
        forward: PaginationButton? = null,
    ) : PaginationTransformation<P, Element>(positionGenerator, default, back, forward) {
        override suspend fun drawElement(index: Int, element: Element): Element = element
    }

    /** A list of positions for this transformation. */
    protected val positions: List<GridPoint> = positionGenerator.generate()

    /** The number of slots. */
    protected val slots: Int = positions.size

    /** The values this transformation is displaying. */
    protected var values: List<E> by Delegates.observable(default.toList()) { _, _, _ ->
        boundPage.max = maxPages()
        refreshTrigger.trigger()
    }

    /** The number of entries that need pages available to them. */
    protected open val entryCount: Int
        get() = values.lastIndex

    init {
        boundPage.max = maxPages()
    }

    override suspend fun invoke(pane: P, view: InterfaceView) {
        val positions = positionGenerator.generate()
        val slots = positions.size

        val offset = page * slots

        positions.forEachIndexed { index, point ->
            val currentIndex = index + offset

            if (currentIndex >= values.size) {
                return@forEachIndexed
            }

            pane[point] = drawElement(index, values[currentIndex])
        }

        super.invoke(pane, view)
    }

    /** Draws an element. */
    protected abstract suspend fun drawElement(index: Int, element: E): Element

    /** Returns the maximum number of pages required to accommodate all the entries in [values]. */
    private fun maxPages(): Int {
        return entryCount.floorDiv(positionGenerator.generate().size).coerceAtLeast(0)
    }
}
