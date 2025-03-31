package com.noxcrew.interfaces.inventory

import com.noxcrew.interfaces.grid.mapping.PlayerInventoryGridMapper
import com.noxcrew.interfaces.pane.PlayerPane.Companion.BOOTS_SLOT
import com.noxcrew.interfaces.pane.PlayerPane.Companion.CHEST_SLOT
import com.noxcrew.interfaces.pane.PlayerPane.Companion.EXTRA_ROW
import com.noxcrew.interfaces.pane.PlayerPane.Companion.HELMET_SLOT
import com.noxcrew.interfaces.pane.PlayerPane.Companion.LEGGING_SLOT
import com.noxcrew.interfaces.pane.PlayerPane.Companion.OFFHAND_ROW
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/** A wrapper around a [PlayerInventory]. */
public class PlayerInterfacesInventory(
    private val player: Player,
    private val mapper: PlayerInventoryGridMapper,
) : CachedInterfacesInventory() {

    private val playerInventory = player.inventory

    override fun get(row: Int, column: Int): ItemStack? {
        // First handle the offhand slot.
        if (row == OFFHAND_ROW) {
            return playerInventory.itemInOffHand
        }

        // Handle crafting and armor.
        if (row == EXTRA_ROW) {
            return when (column) {
                // Armor slots.
                HELMET_SLOT.y -> playerInventory.helmet
                CHEST_SLOT.y -> playerInventory.chestplate
                LEGGING_SLOT.y -> playerInventory.leggings
                BOOTS_SLOT.y -> playerInventory.boots

                // In the future could add support for crafting grid slots.
                else -> null
            }
        }

        // Lastly, all normal items.
        return playerInventory.getItem(mapper.toPlayerInventorySlot(row, column))
    }

    override fun setInternal(row: Int, column: Int, item: ItemStack?) {
        // First handle the offhand slot.
        if (row == OFFHAND_ROW) {
            playerInventory.setItemInOffHand(item)
            return
        }

        // Handle crafting and armor.
        if (row == EXTRA_ROW) {
            when (column) {
                // Armor slots.
                HELMET_SLOT.y -> playerInventory.helmet = item
                CHEST_SLOT.y -> playerInventory.chestplate = item
                LEGGING_SLOT.y -> playerInventory.leggings = item
                BOOTS_SLOT.y -> playerInventory.boots = item
            }
            return
        }

        // Lastly, all normal items.
        playerInventory.setItem(mapper.toPlayerInventorySlot(row, column), item)
    }

    override fun isPlayerInventory(row: Int, column: Int): Boolean = true
}
