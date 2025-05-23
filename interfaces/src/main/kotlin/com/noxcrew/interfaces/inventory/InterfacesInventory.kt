package com.noxcrew.interfaces.inventory

import org.bukkit.inventory.ItemStack

/** Represents some inventory that item stacks can be placed into. */
public interface InterfacesInventory {

    /** Sets the item at ([row], [column]) to [item]. */
    public fun set(row: Int, column: Int, item: ItemStack?): Boolean

    /** Returns the value at ([row], [column]). */
    public fun get(row: Int, column: Int): ItemStack?
}
