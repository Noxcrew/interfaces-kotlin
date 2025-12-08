package com.noxcrew.interfaces.utilities

import com.noxcrew.interfaces.grid.mapping.GridMapper
import com.noxcrew.interfaces.pane.CompletedPane
import com.noxcrew.interfaces.pane.createEmptyPane

/** A collection of completed panes that can be collapsed to create a new merged [CompletedPane]. */
internal class CollapsablePaneMap private constructor(
    // privately pass in a map here so that we can use
    // super methods when overriding methods in the delegate.
    private val internal: MutableMap<Int, CompletedPane>
) : MutableMap<Int, CompletedPane> by internal {

    internal companion object {
        /** Creates a new collapsable map of panes. */
        internal fun create() = CollapsablePaneMap(sortedMapOf(Comparator.naturalOrder()))
    }

    private var cachedPane: CompletedPane? = null

    override fun put(key: Int, value: CompletedPane): CompletedPane? {
        cachedPane = null
        return internal.put(key, value)
    }

    internal fun collapse(mapper: GridMapper, allowClickingEmptySlots: Boolean): CompletedPane {
        cachedPane?.let { pane ->
            return pane
        }

        val pane = createEmptyPane(mapper, allowClickingEmptySlots)
        val current = internal.toMap().values
        current.forEach { layer ->
            layer.forEach { row, column, value ->
                pane[row, column] = value
            }
        }
        cachedPane = pane
        return pane
    }
}
