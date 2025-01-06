package com.noxcrew.interfaces.pane

import com.noxcrew.interfaces.element.Element
import com.noxcrew.interfaces.grid.GridMap
import com.noxcrew.interfaces.grid.HashGridMap

/** A grid map of elements. */
public open class Pane : GridMap<Element> {

    // This has to be manual redirecting instead of using the by
    // syntax as the by syntax breaks the ability to override
    // any of the methods!

    private val gridMap = HashGridMap<Element>()

    override fun set(row: Int, column: Int, value: Element): Unit =
        gridMap.set(row, column, value)

    override fun get(row: Int, column: Int): Element? =
        gridMap[row, column]

    override fun has(row: Int, column: Int): Boolean =
        gridMap.has(row, column)

    override suspend fun forEachSuspending(consumer: suspend (row: Int, column: Int, Element) -> Unit): Unit =
        gridMap.forEachSuspending(consumer)

    override fun forEach(consumer: (row: Int, column: Int, Element) -> Unit): Unit =
        gridMap.forEach(consumer)
}
