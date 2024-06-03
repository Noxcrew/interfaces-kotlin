package com.noxcrew.interfaces.pane

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.element.CompletedElement
import com.noxcrew.interfaces.element.complete
import com.noxcrew.interfaces.grid.GridMap
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.grid.HashGridMap
import com.noxcrew.interfaces.utilities.forEachInGrid
import com.noxcrew.interfaces.view.AbstractInterfaceView.Companion.COLUMNS_IN_CHEST
import org.bukkit.entity.Player

/** A grid map of completed elements. */
internal open class CompletedPane : GridMap<CompletedElement> by HashGridMap() {
    internal open fun getRaw(vector: GridPoint): CompletedElement? = get(vector)

    internal open fun getRawUnordered(vector: GridPoint): CompletedElement? = get(vector)
}

/** A completed pane with an ordering. */
internal class CompletedOrderedPane(
    private val ordering: List<Int>
) : CompletedPane() {

    override fun getRaw(vector: GridPoint): CompletedElement? =
        ordering.getOrNull(vector.x)?.let { get(it, vector.y) }
}

/** Completes a pane for [player] by drawing each element while suspending. */
internal suspend fun Pane.complete(player: Player): CompletedPane {
    val pane = convertToEmptyCompletedPane()

    forEachSuspending { row, column, element ->
        pane[row, column] = element.complete(player)
    }

    return pane
}

/** Fills up a completed pane with empty elements. */
internal fun Pane.convertToEmptyCompletedPaneAndFill(rows: Int): CompletedPane {
    val pane = convertToEmptyCompletedPane()
    val airElement = CompletedElement(null, ClickHandler.EMPTY)

    forEachInGrid(rows, COLUMNS_IN_CHEST) { row, column ->
        pane[row, column] = airElement
    }

    return pane
}

/** Converts this pane to either a [CompletedPane] or [CompletedOrderedPane] based on its type. */
internal fun Pane.convertToEmptyCompletedPane(): CompletedPane {
    if (this is OrderedPane) {
        return CompletedOrderedPane(ordering)
    }
    return CompletedPane()
}
