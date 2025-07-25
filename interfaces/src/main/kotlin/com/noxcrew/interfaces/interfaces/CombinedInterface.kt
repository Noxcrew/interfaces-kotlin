package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.grid.mapping.CombinedGridMapper
import com.noxcrew.interfaces.pane.CombinedPane
import com.noxcrew.interfaces.view.CombinedInterfaceView
import com.noxcrew.interfaces.view.InterfaceView
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** An interface that uses a chest GUI and the player's inventory below. */
public class CombinedInterface internal constructor(
    override val rows: Int,
    override val titleSupplier: (suspend (Player) -> Component?)?,
    override val builder: CombinedInterfaceBuilder,
) : Interface<CombinedInterface, CombinedPane>, TitledInterface {

    override val includesPlayerInventory: Boolean = true
    override val mapper: CombinedGridMapper = CombinedGridMapper(rows)

    override fun createPane(): CombinedPane = CombinedPane(rows)

    override suspend fun open(player: Player, parent: InterfaceView?, reload: Boolean): CombinedInterfaceView {
        val view = CombinedInterfaceView(player, this, parent)
        view.open(reload)
        return view
    }
}
