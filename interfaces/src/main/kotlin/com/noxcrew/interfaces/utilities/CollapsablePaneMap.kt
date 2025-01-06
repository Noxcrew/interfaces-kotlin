package com.noxcrew.interfaces.utilities

import com.noxcrew.interfaces.pane.CompletedPane
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.pane.convertToEmptyCompletedPane
import com.noxcrew.interfaces.pane.convertToEmptyCompletedPaneAndFill

/** A collection of completed panes that can be collapsed to create a new merged [CompletedPane]. */
internal class CollapsablePaneMap private constructor(
    // pass in a pane from the view so that we can persist
    // ordering, used in the listeners.
    private val basePane: Pane,
    // privately pass in a map here so that we can use
    // super methods when overriding methods in the delegate.
    private val internal: MutableMap<Int, CompletedPane>
) : MutableMap<Int, CompletedPane> by internal {

    internal companion object {
        /** Creates a new collapsable map with [basePane]. */
        internal fun create(basePane: Pane) = CollapsablePaneMap(
            basePane,
            sortedMapOf(Comparator.reverseOrder())
        )
    }

    private var cachedPane: CompletedPane? = null

    override fun put(key: Int, value: CompletedPane): CompletedPane? {
        cachedPane = null
        return internal.put(key, value)
    }

    internal fun collapse(rows: Int, fill: Boolean): CompletedPane {
        cachedPane?.let { pane ->
            return pane
        }

        val pane = if (fill) basePane.convertToEmptyCompletedPaneAndFill(rows) else basePane.convertToEmptyCompletedPane()
        val current = internal.toMap().values

        current.forEach { layer ->
            layer.forEach { row, column, value ->
                pane[row, column] = value
            }
        }

        return pane
    }
}
