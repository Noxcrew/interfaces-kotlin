package com.noxcrew.interfaces.grid.mapping

import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.grid.GridPoint.Companion.OUTSIDE_CHEST_INDEX

/**
 * Responsible for mapping inventory slots into [GridPoint] and vice versa.
 * @see AbstractGridMapper for the default [toGridPoint] implementation.
 * @see ChestGridMapper for the [TopInventory] only implementation.
 * @see CombinedGridMapper for a combined [TopInventory] and [PlayerInventory] implementation.
 * @see PlayerInventoryGridMapper for the [PlayerInventory] only implementation.
 */
public interface GridMapper {

    /**
     * Runs [function] for every position in the grid.
     */
    public fun forEachInGrid(function: (row: Int, column: Int) -> Unit)

    /**
     * This function is called from the listener where the [slot] is the `rawSlot` representation.
     * This means that slots will go from 0 to x as opposed to normal slots which are based on the inventory they are in.
     */
    public fun toGridPoint(slot: Int): GridPoint?

    /** Whether the [GridPoint] is in the player's inventory. */
    public fun isPlayerInventory(row: Int, column: Int): Boolean

    /** Responsible for mapping [GridPoint] into top inventory slots. */
    public interface TopInventory : GridMapper {
        /** The slot returned should be relative to the inventory it is in. */
        public fun toTopInventorySlot(row: Int, column: Int): Int
    }

    /**
     *  Responsible for mapping [GridPoint] into player inventory slots.
     *  Which cane be completely different based on the type of the [TopInventory].
     */
    public interface PlayerInventory : GridMapper {
        public companion object {
            public const val PLAYER_INV_ROWS: Int = 3

            public const val PLAYER_INV_PLUS_HOTBAR_ROWS: Int = PLAYER_INV_ROWS + 1
        }

        /** The slot returned is relevant to the PlayerInventory mapping, normally 0-8 is the hot bar. */
        public fun toPlayerInventorySlot(row: Int, column: Int): Int
    }
}

/** Simple implementation for [toGridPoint] that can be shared by multiple [GridMapper] (assumes chest-based containers). */
public abstract class AbstractGridMapper : GridMapper {
    override fun toGridPoint(slot: Int): GridPoint? {
        if (slot == OUTSIDE_CHEST_INDEX) return null
        return GridPoint(slot / 9, slot % 9)
    }
}
