package com.noxcrew.interfaces.inventory

import com.noxcrew.interfaces.grid.GridPoint
import org.bukkit.inventory.ItemStack

/** Represents some inventory that item stacks can be placed into. */
public interface InterfacesInventory {

    /** Sets the item at ([row], [column]) to [item]. */
    public fun set(row: Int, column: Int, item: ItemStack?): Boolean

    /** Sets the item at [gridPoint] to [item]. */
    public fun set(gridPoint: GridPoint, item: ItemStack?): Boolean = set(gridPoint.x, gridPoint.y, item)

    /** Returns the value at ([row], [column]). */
    public fun get(row: Int, column: Int): ItemStack?

    /** Returns the value at [gridPoint]. */
    public fun get(gridPoint: GridPoint): ItemStack? = get(gridPoint.x, gridPoint.y)
}
