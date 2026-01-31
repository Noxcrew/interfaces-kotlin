package com.noxcrew.interfaces.pane

import com.noxcrew.interfaces.element.Element
import com.noxcrew.interfaces.grid.GridPoint

/** A combined pane where a given amount of chest rows are followed by the 4 player inventory rows. */
public class CombinedPane(
    /** The amount of rows of chest at the top of the pane. */
    public val chestRows: Int,
) : Pane() {
    private val offHandSlot: GridPoint = GridPoint(chestRows + 4, 0)

    /** The off hand slot of the player inventory. */
    public var offHand: Element
        get() = get(offHandSlot) ?: Element.EMPTY
        set(value) = set(offHandSlot, value)
}
