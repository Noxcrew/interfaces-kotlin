package com.noxcrew.interfaces.menu

import com.noxcrew.interfaces.exception.InterfacesExceptionContext
import com.noxcrew.interfaces.exception.InterfacesExceptionHandler
import com.noxcrew.interfaces.exception.InterfacesOperation
import com.noxcrew.interfaces.exception.StandardInterfacesExceptionHandler
import com.noxcrew.interfaces.interfaces.PlayerInterface
import com.noxcrew.interfaces.interfaces.PlayerInterfaceBuilder
import com.noxcrew.interfaces.interfaces.buildPlayerInterface
import com.noxcrew.interfaces.utilities.InterfacesProfiler
import com.noxcrew.interfaces.view.InterfaceView
import com.noxcrew.interfaces.view.PlayerInterfaceView
import org.bukkit.entity.Player
import java.time.Instant

/** The base for a menu that is applied on a player's whole inventory. */
public abstract class PlayerInventoryMenu : BaseInventoryMenu {
    /** The exception handler to use for this menu. */
    public open val exceptionHandler: InterfacesExceptionHandler = StandardInterfacesExceptionHandler()

    /** Enables debug logs for time spent on various actions. */
    public open val debugRenderingTime: Boolean = false

    /** Configures the GUI for the given [player]. */
    protected abstract suspend fun PlayerInterfaceBuilder.configure(player: Player)

    /** Builds the menu. */
    protected open suspend fun buildMenu(player: Player): PlayerInterface = buildPlayerInterface {
        this@buildPlayerInterface.exceptionHandler = this@PlayerInventoryMenu.exceptionHandler
        this@buildPlayerInterface.debugRenderingTime = this@PlayerInventoryMenu.debugRenderingTime
        configure(player)
    }

    override suspend fun open(player: Player, parent: InterfaceView?, reload: Boolean): PlayerInterfaceView? {
        val start = Instant.now()
        val menu = exceptionHandler.execute(
            InterfacesExceptionContext(
                player,
                InterfacesOperation.BUILDING_PLAYER,
                null,
            ),
        ) { buildMenu(player) } ?: return null
        if (debugRenderingTime) InterfacesProfiler.logAbstract("building menu", start)
        return open(player, parent, menu, reload) as PlayerInterfaceView?
    }
}
