package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.grid.mapping.ChestGridMapper
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.view.ChestInterfaceView
import com.noxcrew.interfaces.view.InterfaceView
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** An interface that uses a chest GUI. */
public class ChestInterface internal constructor(
    override val rows: Int,
    override val titleSupplier: (suspend (Player) -> Component?)?,
    override val builder: ChestInterfaceBuilder
) : Interface<ChestInterface, Pane>, TitledInterface {

    public companion object {
        /** The maximum number of rows for a chest GUI. */
        public const val MAX_NUMBER_OF_ROWS: Int = 6
    }

    override val includesPlayerInventory: Boolean = false
    override val mapper: ChestGridMapper = ChestGridMapper(rows)

    override fun createPane(): Pane = Pane()

    override suspend fun open(player: Player, parent: InterfaceView?, reload: Boolean): ChestInterfaceView {
        val view = ChestInterfaceView(player, this, parent)
        view.open(reload)
        return view
    }
}
