package com.noxcrew.interfaces.grid.mapping

import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.grid.GridPoint.Companion.OUTSIDE_CHEST_INDEX

public interface GridMapper {

    /**
     * This function is called from the listener where the [slot] is the `rawSlot` representation.
     * This means that slots will go from 0 to x as opposed to normal slots which are based on the inventory they are in.
     */
    public fun toGridPoint(slot: Int): GridPoint?

    public fun isPlayerInventory(row: Int, column: Int): Boolean

    public interface TopInventory : GridMapper {
        /** The slot returned should be relative to the inventory it is in. */
        public fun toTopInventorySlot(row: Int, column: Int): Int
    }

    public interface PlayerInventory : GridMapper {
        public companion object {
            public const val PLAYER_INV_ROWS: Int = 3
        }
        /** The slot returned is relevant to the PlayerInventory mapping, normally 0-8 is the hot bar. */
        public fun toPlayerInventorySlot(row: Int, column: Int): Int
    }
}

public abstract class AbstractGridMapper : GridMapper {
    override fun toGridPoint(slot: Int): GridPoint? {
        if (slot == OUTSIDE_CHEST_INDEX) return null
        return GridPoint(slot / 9, slot % 9)
    }
}
