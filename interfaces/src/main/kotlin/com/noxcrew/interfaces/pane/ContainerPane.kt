package com.noxcrew.interfaces.pane

import com.noxcrew.interfaces.element.Element
import com.noxcrew.interfaces.grid.GridPoint

/** A container pane of [rows] rows followed by the player inventory. */
public open class ContainerPane(
    /** The amount of rows of top inventory at the top of the pane. */
    public val rows: Int,
    /** Whether the player inventory is accessible. */
    public val isPlayerInventoryAccessible: Boolean,
) : Pane() {
    private val offHandSlot: GridPoint = GridPoint(rows + 4, 0)

    /** The off-hand slot of the player inventory. */
    public var offHand: Element
        get() = get(offHandSlot) ?: Element.EMPTY
        set(value) {
            require(isPlayerInventoryAccessible) { "Cannot modify off-hand if no player inventory access is allowed" }
            set(offHandSlot, value)
        }
}
