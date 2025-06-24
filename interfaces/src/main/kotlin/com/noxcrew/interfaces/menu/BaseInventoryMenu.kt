package com.noxcrew.interfaces.menu

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.view.InterfaceView
import kotlinx.coroutines.isActive
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.coroutines.coroutineContext

/** The base for a menu in an inventory. */
public interface BaseInventoryMenu {

    /** Opens this menu for the given [player]. */
    public suspend fun open(
        player: Player,
        parent: InterfaceView? =
            InterfacesListeners.INSTANCE.convertHolderToInterfaceView(player.openInventory.topInventory.getHolder(false)),
        reload: Boolean = true,
    ): InterfaceView?

    /** Opens a rendered [menu] for the given [player]. */
    public suspend fun open(player: Player, parent: InterfaceView?, menu: Interface<*, *>, reload: Boolean = true): InterfaceView? {
        // Quit if the coroutine is no longer active
        if (!coroutineContext.isActive) return null

        // Never open menus for offline players or if we are shutting down.
        if (Bukkit.isStopping() || !player.isConnected) return null

        // Don't open the menu if no sibling menu is open! (parent will be closed, but
        // this can be used to check if some menu is open that shares the parent)
        if (parent?.isTreeOpened == false) return null

        // Finally open the actual menu!
        return menu.open(player, parent, reload)
    }
}
