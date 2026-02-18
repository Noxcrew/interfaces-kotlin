package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.inventory.InterfacesInventory
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player

/** A generic processor run on an inventory. */
public fun interface InventoryProcessor : (InterfacesInventory, InterfaceView, Player) -> Unit
