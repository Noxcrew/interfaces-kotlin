package com.noxcrew.interfaces.pane

import com.noxcrew.interfaces.element.Element

public abstract class OrderedPane(
    internal val ordering: List<Int>
) : Pane() {

    override fun get(row: Int, column: Int): Element? {
        return super.get(orderedRow(row), column)
    }

    override fun set(row: Int, column: Int, value: Element) {
        return super.set(orderedRow(row), column, value)
    }

    override fun has(row: Int, column: Int): Boolean {
        return super.has(orderedRow(row), column)
    }

    private fun orderedRow(row: Int) = ordering[row]
}
