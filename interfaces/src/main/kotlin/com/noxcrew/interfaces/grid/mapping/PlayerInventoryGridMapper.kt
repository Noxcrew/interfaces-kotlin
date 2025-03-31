package com.noxcrew.interfaces.grid.mapping

import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.grid.mapping.GridMapper.PlayerInventory.Companion.PLAYER_INV_ROWS
import com.noxcrew.interfaces.utilities.gridPointToBukkitIndex
import com.noxcrew.interfaces.view.AbstractInterfaceView.Companion.COLUMNS_IN_CHEST

public object PlayerInventoryGridMapper : AbstractGridMapper(), GridMapper.PlayerInventory {

    private const val PLAYER_INV_SIZE = PLAYER_INV_ROWS * COLUMNS_IN_CHEST
    private const val PLAYER_INV_HOT_BAR_SIZE = PLAYER_INV_SIZE + COLUMNS_IN_CHEST

    override fun toGridPoint(slot: Int): GridPoint? {
        // Hot bar slot.
        if (slot >= PLAYER_INV_HOT_BAR_SIZE + COLUMNS_IN_CHEST) {
            return super.toGridPoint(slot)
        }

        // "Extra" slots, armor, offhand, crafting grid.
        if (slot < COLUMNS_IN_CHEST) {
            return super.toGridPoint(slot + PLAYER_INV_HOT_BAR_SIZE)
        }

        // Everything else we just shift it up by 9 slots.
        return super.toGridPoint(slot - COLUMNS_IN_CHEST)
    }

    override fun toPlayerInventorySlot(row: Int, column: Int): Int {
        // If it's not the last row we remove chest size.
        if (row < PLAYER_INV_ROWS) return gridPointToBukkitIndex(row, column) - COLUMNS_IN_CHEST
        // On the last row we use row 0 to map it from 0-8 for the hot bar.
        return gridPointToBukkitIndex(0, column)
    }

    override fun isPlayerInventory(row: Int, column: Int): Boolean = true
}
