package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.PlayerPane
import com.noxcrew.interfaces.view.InterfaceView
import com.noxcrew.interfaces.view.PlayerInterfaceView
import org.bukkit.entity.Player

/** An interface that uses the entire player inventory. */
public class PlayerInterface internal constructor(
    override val builder: PlayerInterfaceBuilder
) : Interface<PlayerInterface, PlayerPane> {

    public companion object {
        /** The maximum number of rows for a player interface. */
        public const val MAX_NUMBER_OF_ROWS: Int = 9
    }

    override val includesPlayerInventory: Boolean = true

    override val rows: Int = 4

    override fun createPane(): PlayerPane = PlayerPane()

    override suspend fun open(player: Player, parent: InterfaceView?): PlayerInterfaceView {
        val view = PlayerInterfaceView(player, this)
        view.open()
        return view
    }
}
