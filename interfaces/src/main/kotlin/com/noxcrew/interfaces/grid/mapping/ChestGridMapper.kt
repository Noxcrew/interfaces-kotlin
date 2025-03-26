package com.noxcrew.interfaces.grid.mapping

import com.noxcrew.interfaces.utilities.gridPointToBukkitIndex

public open class ChestGridMapper(private val rows: Int) : AbstractGridMapper(), GridMapper.TopInventory {

    override fun toTopInventorySlot(row: Int, column: Int): Int {
        return gridPointToBukkitIndex(row, column)
    }

    override fun isPlayerInventory(row: Int, column: Int): Boolean = row >= rows
}
