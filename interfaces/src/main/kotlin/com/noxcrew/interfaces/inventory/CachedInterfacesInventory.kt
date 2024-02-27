package com.noxcrew.interfaces.inventory

import org.bukkit.inventory.ItemStack

/** An [InterfacesInventory] where changes are only applied if the new item stack is different. */
public abstract class CachedInterfacesInventory : InterfacesInventory {

    final override fun set(row: Int, column: Int, item: ItemStack?): Boolean {
        val current = get(row, column)

        if (current == item) {
            return false
        }

        setInternal(row, column, item)
        return true
    }

    /** Sets the item at ([row], [column]) to [item]. */
    protected abstract fun setInternal(row: Int, column: Int, item: ItemStack?)
}
