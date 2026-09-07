package com.noxcrew.interfaces.utilities

import com.noxcrew.interfaces.grid.GridPoint

/** Converts a ([row], [column]) grid point to an index used by Bukkit. */
public fun gridPointToBukkitIndex(row: Int, column: Int): Int {
    return row * 9 + column
}

/** Converts a [GridPoint] to an index used by Bukkit. */
public fun gridPointToBukkitIndex(gridPoint: GridPoint): Int = gridPointToBukkitIndex(gridPoint.x, gridPoint.y)

/** Runs [function] for all values in a grid of [rows] height and [columns] width. */
public fun iterateForEachInGrid(rows: Int, columns: Int, function: (row: Int, column: Int) -> Unit) {
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            function(row, column)
        }
    }
}
