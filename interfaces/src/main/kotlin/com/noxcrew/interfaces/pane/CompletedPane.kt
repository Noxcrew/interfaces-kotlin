package com.noxcrew.interfaces.pane

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.element.CompletedElement
import com.noxcrew.interfaces.element.complete
import com.noxcrew.interfaces.grid.GridMap
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.grid.HashGridMap
import com.noxcrew.interfaces.grid.mapping.GridMapper
import org.bukkit.entity.Player

/** A grid map of completed elements. */
public open class CompletedPane : GridMap<CompletedElement> by HashGridMap() {
    public open fun getRaw(vector: GridPoint): CompletedElement? = get(vector)
}

/** Completes a pane for [player] by drawing each element while suspending. */
internal suspend fun Pane.complete(player: Player): CompletedPane {
    val pane = CompletedPane()

    forEachSuspending { row, column, element ->
        pane[row, column] = element.complete(player)
    }

    return pane
}

/** Creates a completed pane with empty elements. */
internal fun createEmptyPane(mapper: GridMapper, allowClickingEmptySlots: Boolean): CompletedPane {
    val pane = CompletedPane()
    val airElement = CompletedElement(null, if (allowClickingEmptySlots) ClickHandler.EMPTY else null)
    mapper.forEachInGrid { row, column ->
        pane[row, column] = airElement
    }
    return pane
}
