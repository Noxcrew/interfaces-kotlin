package com.noxcrew.interfaces.pane

import com.noxcrew.interfaces.element.Element

/** A pane where row indices are re-ordered using [ordering]. */
public abstract class OrderedPane(
    internal val ordering: List<Int>,
) : Pane() {
    override fun get(
        row: Int,
        column: Int,
    ): Element? = super.get(orderedRow(row), column)

    override fun set(
        row: Int,
        column: Int,
        value: Element,
    ) = super.set(orderedRow(row), column, value)

    override fun has(
        row: Int,
        column: Int,
    ): Boolean = super.has(orderedRow(row), column)

    private fun orderedRow(row: Int) = ordering[row]
}
