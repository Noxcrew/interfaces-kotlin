package com.noxcrew.interfaces.inventory

import com.noxcrew.interfaces.grid.mapping.CombinedGridMapper
import com.noxcrew.interfaces.grid.mapping.GridMapper.PlayerInventory.Companion.PLAYER_INV_PLUS_HOTBAR_ROWS
import com.noxcrew.interfaces.view.AbstractInterfaceView.Companion.COLUMNS_IN_CHEST
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftInventoryCustom
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/** A wrapper around an inventory for a chest and a fake player inventory. */
public class CombinedInterfacesInventory(
    holder: InventoryHolder,
    player: Player,
    private val rows: Int,
    private val mapper: CombinedGridMapper,
) : CachedInterfacesInventory() {

    /** The [playerInventory] to place lower items in without editing the player's current equipment. */
    public val playerInventory: net.minecraft.world.entity.player.Inventory =
        net.minecraft.world.entity.player.Inventory((player as CraftPlayer).handle, EntityEquipment())

    /** The [chestInventory] being used to place items in. */
    public val chestInventory: net.minecraft.world.Container = SimpleContainer(rows * COLUMNS_IN_CHEST, holder)

    /** The bukkit inventory that wraps [chestInventory]. */
    public val bukkitInventory: org.bukkit.inventory.Inventory = CraftInventoryCustom(holder, rows * COLUMNS_IN_CHEST)

    override fun get(row: Int, column: Int): ItemStack? {
        if (row == rows + PLAYER_INV_PLUS_HOTBAR_ROWS) {
            return playerInventory.equipment.get(EquipmentSlot.OFFHAND).asBukkitMirror()
        }

        if (mapper.isPlayerInventory(row, column)) {
            return playerInventory.getItem(mapper.toPlayerInventorySlot(row, column)).asBukkitMirror()
        }

        return chestInventory.getItem(mapper.toTopInventorySlot(row, column)).asBukkitMirror()
    }

    override fun setInternal(row: Int, column: Int, item: ItemStack?) {
        if (row == rows + PLAYER_INV_PLUS_HOTBAR_ROWS) {
            playerInventory.equipment.set(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(item))
            return
        }

        if (mapper.isPlayerInventory(row, column)) {
            playerInventory.setItem(mapper.toPlayerInventorySlot(row, column), CraftItemStack.asNMSCopy(item))
            return
        }

        chestInventory.setItem(mapper.toTopInventorySlot(row, column), CraftItemStack.asNMSCopy(item))
    }
}
