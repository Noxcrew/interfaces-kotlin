package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.inventory.InterfacesInventory
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player

/** A post-processor run on an inventory. */
public fun interface InventoryPostprocessor : (InterfacesInventory, InterfaceView, Player) -> Unit
