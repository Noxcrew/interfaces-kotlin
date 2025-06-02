package com.noxcrew.interfaces.menu

import com.noxcrew.interfaces.exception.InterfacesExceptionContext
import com.noxcrew.interfaces.exception.InterfacesExceptionHandler
import com.noxcrew.interfaces.exception.InterfacesOperation
import com.noxcrew.interfaces.exception.StandardInterfacesExceptionHandler
import com.noxcrew.interfaces.interfaces.PlayerInterfaceBuilder
import com.noxcrew.interfaces.interfaces.buildPlayerInterface
import com.noxcrew.interfaces.view.InterfaceView
import com.noxcrew.interfaces.view.PlayerInterfaceView
import org.bukkit.entity.Player

/** The base for a menu that is applied on a player's whole inventory. */
public abstract class PlayerInventoryMenu : BaseInventoryMenu {
    /** The exception handler to use for this menu. */
    public open val exceptionHandler: InterfacesExceptionHandler = StandardInterfacesExceptionHandler()

    /** Configures the GUI for the given [player]. */
    protected abstract suspend fun PlayerInterfaceBuilder.configure(player: Player)

    override suspend fun open(player: Player, parent: InterfaceView?): PlayerInterfaceView? {
        val menu = exceptionHandler.execute(
            InterfacesExceptionContext(
                player,
                InterfacesOperation.BUILDING_PLAYER,
            ),
        ) {
            buildPlayerInterface {
                configure(player)
            }
        } ?: return null
        return openMenu(player, parent, menu) as PlayerInterfaceView
    }
}
