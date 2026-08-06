package com.noxcrew.interfaces.menu

import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player

/** An operation to open a menu. */
public data class MenuOpenOperation(
    public val menu: BaseInventoryMenu,
    public val parent: InterfaceView?,
) {
    /** Executes this operation. */
    public suspend fun execute(player: Player) {
        menu.reopen(player, parent)
    }
}
