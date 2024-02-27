package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.event.inventory.InventoryCloseEvent

public fun interface CloseHandler : suspend (InventoryCloseEvent.Reason, InterfaceView) -> Unit
