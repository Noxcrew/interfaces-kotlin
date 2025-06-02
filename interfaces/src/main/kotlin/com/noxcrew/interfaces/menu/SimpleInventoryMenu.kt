package com.noxcrew.interfaces.menu

import com.noxcrew.interfaces.exception.InterfacesExceptionContext
import com.noxcrew.interfaces.exception.InterfacesExceptionHandler
import com.noxcrew.interfaces.exception.InterfacesOperation
import com.noxcrew.interfaces.exception.StandardInterfacesExceptionHandler
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.InterfaceBuilder
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player

/** The base for a simple built menu. */
public abstract class SimpleInventoryMenu<B : InterfaceBuilder<out Pane, *>> : BaseInventoryMenu {
    /** The exception handler to use for this menu. */
    public open val exceptionHandler: InterfacesExceptionHandler = StandardInterfacesExceptionHandler

    /** Builds the interface for this type using [configure]. */
    protected abstract suspend fun build(player: Player, configure: suspend B.() -> Unit): Interface<*, *>

    /** Configures the GUI for the given [player]. */
    protected abstract suspend fun B.configure(player: Player)

    override suspend fun open(player: Player, parent: InterfaceView?): InterfaceView? {
        val menu = exceptionHandler.execute(
            InterfacesExceptionContext(
                player,
                InterfacesOperation.BUILDING_REGULAR,
            ),
        ) {
            build(player) {
                configure(player)
            }
        } ?: return null

        return openMenu(player, parent, menu)
    }
}
