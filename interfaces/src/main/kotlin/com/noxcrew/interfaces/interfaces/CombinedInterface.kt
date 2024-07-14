package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.CombinedPane
import com.noxcrew.interfaces.view.CombinedInterfaceView
import com.noxcrew.interfaces.view.InterfaceView
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** An interface that uses a chest GUI and the player's inventory below. */
public class CombinedInterface internal constructor(
    override val rows: Int,
    override val initialTitle: Component?,
    override val builder: CombinedInterfaceBuilder
) : Interface<CombinedInterface, CombinedPane>, TitledInterface {

    public companion object {
        /** The maximum number of rows for a combined interface. */
        public const val MAX_NUMBER_OF_ROWS: Int = 9
    }

    override val includesPlayerInventory: Boolean = true

    override fun totalRows(): Int = rows + 4

    override fun createPane(): CombinedPane = CombinedPane(rows)

    override suspend fun open(player: Player, parent: InterfaceView?): CombinedInterfaceView {
        val view = CombinedInterfaceView(player, this, parent)
        view.open()
        return view
    }
}
