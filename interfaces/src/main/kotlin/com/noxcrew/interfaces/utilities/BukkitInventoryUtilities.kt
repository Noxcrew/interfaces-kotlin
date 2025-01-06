package com.noxcrew.interfaces.utilities

import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.view.AbstractInterfaceView.Companion.COLUMNS_IN_CHEST
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/** Converts a ([row], [column]) grid point to an index used by Bukkit. */
public fun gridPointToBukkitIndex(row: Int, column: Int): Int {
    return row * 9 + column
}

/** Converts a [GridPoint] to an index used by Bukkit. */
public fun gridPointToBukkitIndex(gridPoint: GridPoint): Int = gridPointToBukkitIndex(gridPoint.x, gridPoint.y)

/** Runs [function] for all values in a grid of [rows] height and [columns] width. */
public fun forEachInGrid(rows: Int, columns: Int, function: (row: Int, column: Int) -> Unit) {
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            function(row, column)
        }
    }
}

/** Creates a new  */
public fun createBukkitInventory(holder: InventoryHolder, rows: Int, title: Component?): Inventory {
    if (title == null) {
        return Bukkit.createInventory(holder, rows * COLUMNS_IN_CHEST)
    }

    return Bukkit.createInventory(holder, rows * COLUMNS_IN_CHEST, title)
}
