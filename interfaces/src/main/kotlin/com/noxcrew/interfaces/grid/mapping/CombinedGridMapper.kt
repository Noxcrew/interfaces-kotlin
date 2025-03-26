package com.noxcrew.interfaces.grid.mapping

import com.noxcrew.interfaces.utilities.gridPointToBukkitIndex
import com.noxcrew.interfaces.view.AbstractInterfaceView.Companion.COLUMNS_IN_CHEST

public class CombinedGridMapper(private val rows: Int) : ChestGridMapper(rows), GridMapper.PlayerInventory {

    private companion object {
        private const val PLAYER_INV_ROWS = 3
    }

    /** Rows of chest times [COLUMNS_IN_CHEST] minus [COLUMNS_IN_CHEST] because 0-8 is in the hot bar. */
    private val chestSize = rows * COLUMNS_IN_CHEST - COLUMNS_IN_CHEST

    override fun toPlayerInventorySlot(row: Int, column: Int): Int {
        // If it's not the last row we remove chest size.
        if (row < rows + PLAYER_INV_ROWS) return gridPointToBukkitIndex(row, column) - chestSize
        // On the last row we use row 0 to map it from 0-8 for the hot bar.
        return gridPointToBukkitIndex(0, column)
    }
}
