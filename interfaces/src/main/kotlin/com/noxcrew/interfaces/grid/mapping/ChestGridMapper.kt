package com.noxcrew.interfaces.grid.mapping

import com.noxcrew.interfaces.utilities.gridPointToBukkitIndex
import com.noxcrew.interfaces.view.AbstractInterfaceView

/** Handles [com.noxcrew.interfaces.grid.GridPoint] mapping for [org.bukkit.event.inventory.InventoryType.CHEST] containers. */
public open class ChestGridMapper(private val rows: Int) : AbstractGridMapper(), GridMapper.TopInventory {

    override fun forEachInGrid(function: (row: Int, column: Int) -> Unit) {
        com.noxcrew.interfaces.utilities.forEachInGrid(rows, AbstractInterfaceView.COLUMNS_IN_CHEST, function)
    }

    override fun toTopInventorySlot(row: Int, column: Int): Int {
        return gridPointToBukkitIndex(row, column)
    }

    override fun isPlayerInventory(row: Int, column: Int): Boolean = row >= rows
}
